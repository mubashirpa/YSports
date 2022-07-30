package ysports.app.fcm

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ysports.app.MainActivity
import ysports.app.R
import ysports.app.util.NotificationUtil

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private var notificationId = 100

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId: String = getString(R.string.default_notification_channel_id)
            val channelName: String = getString(R.string.default_notification_channel_name)
            val channelDescription = getString(R.string.default_notification_channel_description)
            NotificationUtil(this).createNotificationChannel(channelName, channelDescription, channelId, NotificationManager.IMPORTANCE_HIGH)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun sendNotification(messageTitle: String?, messageBody: String?) {
        notificationId++
        val channelId: String = getString(R.string.default_notification_channel_id)
        // Create an explicit intent for an Activity in your app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationUtil(this).createNotification(
                channelId, messageTitle!!, messageBody!!, NotificationManager.IMPORTANCE_HIGH, pendingIntent, notificationId
            )
        } else {
            NotificationUtil(this).createNotification(
                channelId, messageTitle!!, messageBody!!, NotificationCompat.PRIORITY_HIGH, pendingIntent, notificationId
            )
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}