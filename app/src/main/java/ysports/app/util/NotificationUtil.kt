package ysports.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ysports.app.R

class NotificationUtil(val context: Context) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(
        channelName: String, channelDescription: String, channelId: String, channelImportance: Int
    ): String {
        val channel = NotificationChannel(channelId, channelName, channelImportance)
        channel.description = channelDescription
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(channelId) == null) notificationManager.createNotificationChannel(
            channel
        )
        return channelId
    }

    fun sendNotification(
        channelId: String, title: String, message: String, priority: Int, intent: PendingIntent
    ) {
        val builder =
            NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(message).setPriority(priority)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(intent).setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(getUniqueId(), builder.build())
        }
    }

    fun sendNotification(
        title: String,
        message: String,
    ) {
        val channelId: String = context.getString(R.string.default_notification_channel_id)
        val priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_DEFAULT
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }
        val builder =
            NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(message).setPriority(priority)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(getUniqueId(), builder.build())
        }
    }

    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())
}