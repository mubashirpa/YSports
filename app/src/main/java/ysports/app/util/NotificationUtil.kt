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
        channelName: String,
        channelDescription: String,
        channelId: String,
        channelImportance: Int
    ) : String {
        val channel = NotificationChannel(channelId, channelName, channelImportance)
        channel.description = channelDescription
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(channelId) != null)
            notificationManager.createNotificationChannel(channel)
        return channelId
    }

    fun sendNotification(
        channelId: String,
        title: String,
        message: String,
        priority: Int,
        intent: PendingIntent
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(intent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(getUniqueId(), builder.build())
        }
    }

    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())

    private fun areNotificationEnabled() : Boolean {
        return true
    }
}