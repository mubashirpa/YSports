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
        name: String,
        description: String,
        id: String,
        importance: Int
    ) : String {
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return id
    }

    fun createNotification(
        id: String,
        title: String,
        message: String,
        priority: Int,
        intent: PendingIntent,
        notificationID: Int
    ) {
        val builder = NotificationCompat.Builder(context, id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(intent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationID, builder.build())
        }
    }
}