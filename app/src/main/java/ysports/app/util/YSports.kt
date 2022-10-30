package ysports.app.util

import android.app.Application
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import ysports.app.R

class YSports : Application() {

    override fun onCreate() {
        super.onCreate()
        val settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Dynamic color
        val applyDynamicColor = settingsPreferences.getBoolean("dynamic_color", false)
        if (applyDynamicColor) DynamicColors.applyToActivitiesIfAvailable(this)

        // Theme
        when (settingsPreferences.getString("theme", "system_default")) {
            "light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId: String = getString(R.string.default_notification_channel_id)
            val channelName: String = getString(R.string.default_notification_channel_name)
            val channelDescription = getString(R.string.default_notification_channel_description)
            NotificationUtil(this).createNotificationChannel(
                channelName,
                channelDescription,
                channelId,
                NotificationManager.IMPORTANCE_HIGH
            )
        }
    }
}