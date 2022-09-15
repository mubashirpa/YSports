package ysports.app

import android.app.Application
import com.google.android.material.color.DynamicColors

class YSports : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}