package com.voxly.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.voxly.app.MainActivity
import com.voxly.app.R
import com.voxly.app.data.model.User
import com.voxly.app.data.model.Call
import com.voxly.app.ui.call.IncomingCallActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallForegroundService : Service() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var auth: FirebaseAuth

    // private var incomingCallListener: ListenerRegistration? = null // Removed

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            goOfflineAndStop()
            return START_NOT_STICKY
        }

        startForegroundService()

        startListeningForIncomingCalls()
        startHeartbeat()

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        goOfflineAndStop()
    }

    private fun goOfflineAndStop() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            if (com.voxly.app.BuildConfig.DEBUG) {
                android.util.Log.d("ONLINE_TRACE", "WRITER=CallForegroundService.goOfflineAndStop online=false lastSeen=N/A")
            }
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "online" to false,
                        "isOnline" to false,
                        "presence" to "OFFLINE"
                    )
                )
                .addOnSuccessListener {
                    if (com.voxly.app.BuildConfig.DEBUG) {
                        android.util.Log.d("ONLINE_TRACE", "WRITER=CallForegroundService.goOfflineAndStop SUCCESS")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ONLINE_TRACE", "WRITER=CallForegroundService.goOfflineAndStop FAILED", e)
                }
                .addOnCompleteListener {
                    stopSelf()
                }
        } else {
            stopSelf()
        }
    }

    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                updateLastSeen()
                // Update every 60 seconds for robust online status (Reduces writes by 75%)
                kotlinx.coroutines.delay(60 * 1000L) 
            }
        }
    }

    private fun updateLastSeen() {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        if (com.voxly.app.BuildConfig.DEBUG) {
            android.util.Log.d("ONLINE_TRACE", "WRITER=CallForegroundService.updateLastSeen online=N/A lastSeen=$now")
        }
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "lastSeen" to now,
                    "isOnline" to true,
                    "online" to true
                )
            )
            .addOnSuccessListener {
                if (com.voxly.app.BuildConfig.DEBUG) {
                    android.util.Log.d("ONLINE_TRACE", "WRITER=CallForegroundService.updateLastSeen SUCCESS")
                }
            }
            .addOnFailureListener { e -> 
                android.util.Log.e("ONLINE_TRACE", "WRITER=CallForegroundService.updateLastSeen FAILED", e)
                e.printStackTrace() 
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // incomingCallListener?.remove()
        heartbeatJob?.cancel()
        incomingCallListenerJob?.cancel()
    }

    private fun startForegroundService() {
        val stopIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voxly Speaker Mode")
            .setContentText("You are online and receiving calls")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call) // Use system icon
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Go Offline", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC // Default to dataSync or none
            
            val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (hasCamera) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                
                // On SDK 34+ (Android 14), we MUST NOT request camera/mic if permissions missing,
                // otherwise it throws SecurityException. We'll start with what we have.
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                // SDK 29-30
                if (hasMic) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                startForeground(NOTIFICATION_ID, notification, type)
            }
        } else {
            // Below SDK 29
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Inject
    lateinit var callRepository: com.voxly.app.data.repository.CallRepository

    private var currentCallId: String? = null
    private var incomingCallListenerJob: kotlinx.coroutines.Job? = null

    private fun startListeningForIncomingCalls() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        // Use CallRepository to listen for calls (SSOT)
        incomingCallListenerJob?.cancel()
        incomingCallListenerJob = serviceScope.launch {
            callRepository.listenForIncomingCalls(uid).collect { call ->
                android.util.Log.d("CallForegroundService", "Incoming call update: ${call?.id} status=${call?.status}")
                if (call != null && call.status == "ringing") {
                     if (currentCallId != call.id) {
                         android.util.Log.d("CallForegroundService", "Launching IncomingCallActivity for new call: ${call.id}")
                         currentCallId = call.id
                         launchIncomingCallScreen(call)
                     } else {
                         android.util.Log.d("CallForegroundService", "Ignoring duplicate launch for call: ${call.id}")
                     }
                } else {
                    currentCallId = null // Reset when no ringing call
                }
            }
        }
    }

    private fun launchIncomingCallScreen(call: Call) {
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("CALLER_NAME", call.callerName)
            putExtra("CALLER_ID", call.callerId) // Added
            putExtra("CALLER_AVATAR", call.callerAvatar)
            putExtra("RATE", call.rate)
            putExtra("CALL_ID", call.id)
            putExtra("TYPE", call.type)
        }
        
        // Full Screen Intent for Lock Screen
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("Incoming Call")
            .setContentText("You have a new listener!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true) // IMPORTANT: Wakes screen
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_INCOMING, notification)
    }





    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Speaker Online Service",
                NotificationManager.IMPORTANCE_LOW
            )
            
            val callChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming calls"
                // setSound(uri, audioAttributes) // Default sound for now
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "VoxlySpeakerServiceChannel"
        const val CHANNEL_ID_CALLS = "VoxlyIncomingCallsChannel"
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_ID_INCOMING = 2
        const val ACTION_STOP_SERVICE = "STOP_VOXLY_SERVICE"
    }
}
