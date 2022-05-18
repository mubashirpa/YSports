package ysports.app.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import ysports.app.R

class AppUtil(
    val context: Context
) {

    fun openCustomTabs(url: String) {
        val colorInt: Int = ContextCompat.getColor(context, R.color.primary)
        val defaultColors: CustomTabColorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(colorInt)
            .build()
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setDefaultColorSchemeParams(defaultColors)
        val customTabsIntent = builder.build()
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load url", Toast.LENGTH_LONG).show()
        }
    }
}