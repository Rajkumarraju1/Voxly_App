package com.voxly.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.voxly.app.MainActivity
import com.voxly.app.R
import com.voxly.app.data.repository.UserRepository
import com.voxly.app.ui.call.IncomingCallActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
             try {
                 userRepository.updateFcmToken(token)
             } catch (e: Exception) {
                 Log.e(TAG, "Failed into update token", e)
             }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // sendNotification(it.body!!)
        }
    }

    private fun handleNow(data: Map<String, String>) {
        val type = data["type"]
        if (type == "call") {
            val callId = data["callId"] ?: return
            val callerId = data["callerId"] ?: return
            
            // Fix: Prevent caller from receiving their own call notification
            // This happens if the device has a stale token for another user or shared testing device
            val currentUserId = userRepository.getCurrentUserId()
            if (currentUserId == callerId) {
                Log.d(TAG, "Ignoring incoming call notification for own call (CallerId: $callerId)")
                return
            }
            val callerName = data["callerName"] ?: "Unknown"
            val callerAvatar = data["callerAvatar"] ?: ""
            val rate = data["rate"]?.toIntOrNull() ?: 0
            val callType = data["callType"] ?: "audio"

            // Launch Incoming Call Activity
            val intent = Intent(this, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("CALL_ID", callId)
                putExtra("CALLER_ID", callerId)
                putExtra("CALLER_NAME", callerName)
                putExtra("CALLER_AVATAR", callerAvatar)
                putExtra("RATE", rate)
                putExtra("TYPE", callType)
            }
            startActivity(intent)
        }
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback to system icon if local not found
            .setContentTitle(getString(R.string.fcm_message))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
