package com.rkdevstudios.voxly.service

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
import com.rkdevstudios.voxly.MainActivity
import com.rkdevstudios.voxly.R
import com.rkdevstudios.voxly.data.model.User
import com.rkdevstudios.voxly.data.model.Call
import com.rkdevstudios.voxly.ui.call.IncomingCallActivity
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

    @Inject
    lateinit var userRepository: com.rkdevstudios.voxly.data.repository.UserRepository

    @Inject
    lateinit var callSessionManager: com.rkdevstudios.voxly.data.session.CallSessionManager

    // private var incomingCallListener: ListenerRegistration? = null // Removed

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallForegroundService",
            event = "onCreate",
            effect = "Service created",
            callId = currentCallId
        )
        createNotificationChannel()

        serviceScope.launch {
            callSessionManager.sessionState.collect { state ->
                val call = callSessionManager.currentCall.value
                val isVideo = call?.type == "video"
                val hasCall = state !is com.rkdevstudios.voxly.data.session.SessionState.Idle &&
                              state !is com.rkdevstudios.voxly.data.session.SessionState.Ended

                android.util.Log.d("CallForegroundService", "sessionState changed to $state | hasCall=$hasCall")
                
                updateForegroundServiceType(hasCall = hasCall, isVideo = isVideo)

                if (state is com.rkdevstudios.voxly.data.session.SessionState.Ended) {
                    val user = userRepository.currentUserFlow.value
                    val isSpeaker = user?.isSpeaker == true
                    val isOnline = user?.isOnline == true
                    if (!isSpeaker || !isOnline) {
                        com.rkdevstudios.voxly.util.CallBgAudit.log(
                            component = "CallForegroundService",
                            event = "sessionState_ended_stopping_service",
                            effect = "userIsSpeaker=$isSpeaker | isOnline=$isOnline | Stopping service",
                            callId = currentCallId
                        )
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val callId = intent?.getStringExtra("CALL_ID")
        if (callId != null) {
            currentCallId = callId
        }
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallForegroundService",
            event = "onStartCommand",
            effect = "action=$action | flags=$flags | startId=$startId | callId=$callId",
            callId = currentCallId
        )
        if (action == ACTION_STOP_SERVICE) {
            goOfflineAndStop()
            return START_NOT_STICKY
        }

        startForegroundService()

        val user = userRepository.currentUserFlow.value
        if (user != null && user.isSpeaker && user.isOnline) {
            startListeningForIncomingCalls()
            startHeartbeat()
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallForegroundService",
            event = "onTaskRemoved",
            effect = "Task swiped away from recent apps",
            callId = currentCallId
        )
        goOfflineAndStop()
    }

    private fun goOfflineAndStop() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
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
                    if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
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
        if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
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
                if (com.rkdevstudios.voxly.BuildConfig.DEBUG) {
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
        com.rkdevstudios.voxly.util.CallBgAudit.log(
            component = "CallForegroundService",
            event = "onDestroy",
            effect = "Service destroyed",
            callId = currentCallId
        )
        // incomingCallListener?.remove()
        heartbeatJob?.cancel()
        incomingCallListenerJob?.cancel()
    }



    @Inject
    lateinit var callRepository: com.rkdevstudios.voxly.data.repository.CallRepository

    private var currentCallId: String? = null
    private var incomingCallListenerJob: kotlinx.coroutines.Job? = null

    private fun startForegroundService() {
        updateForegroundServiceType(hasCall = false, isVideo = false)
    }

    private fun updateForegroundServiceType(hasCall: Boolean, isVideo: Boolean) {
        val stopIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voxly Speaker Mode")
            .setContentText(if (hasCall) "Active call in progress" else "You are online and receiving calls")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call) // Use system icon
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Go Offline", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                
                if (hasCall) {
                    val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasMic) {
                        type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    }
                    
                    if (isVideo) {
                        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasCamera) {
                            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                        }
                    }
                }
                
                com.rkdevstudios.voxly.util.CallBgAudit.log(
                    component = "CallForegroundService",
                    event = "startForeground",
                    effect = "id=$NOTIFICATION_ID | requestedType=$type | hasMicPermission=${androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED} | hasCameraPermission=${androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED}",
                    callId = currentCallId
                )
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                com.rkdevstudios.voxly.util.CallBgAudit.log(
                    component = "CallForegroundService",
                    event = "startForeground",
                    effect = "id=$NOTIFICATION_ID | fallback Legacy",
                    callId = currentCallId
                )
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            com.rkdevstudios.voxly.util.CallBgAudit.log(
                component = "CallForegroundService",
                event = "startForeground_exception",
                effect = "${e.message}",
                callId = currentCallId
            )
            android.util.Log.e("CallForegroundService", "Failed to start foreground service", e)
        }
    }

    private fun startListeningForIncomingCalls() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        // Use CallRepository to listen for calls (SSOT)
        incomingCallListenerJob?.cancel()
        incomingCallListenerJob = serviceScope.launch {
            callRepository.listenForIncomingCalls(uid).collect { call ->
                android.util.Log.d("CallForegroundService", "Incoming call update: ${call?.id} status=${call?.status}")
                if (call != null) {
                    if (call.status == "ringing") {
                         if (currentCallId != call.id) {
                             android.util.Log.d("CallForegroundService", "Launching IncomingCallActivity for new call: ${call.id}")
                             currentCallId = call.id
                             launchIncomingCallScreen(call)
                         }
                    } else if (call.status == "ended" || call.status == "completed" || call.status == "rejected") {
                         currentCallId = null
                    }
                    
                    // Dynamic promotion: Only request Camera/Mic foreground type during active call
                    val hasActiveCall = call.status == "ringing" || call.status == "active"
                    updateForegroundServiceType(hasCall = hasActiveCall, isVideo = call.type == "video")
                } else {
                    currentCallId = null // Reset when no ringing call
                    updateForegroundServiceType(hasCall = false, isVideo = false)
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
