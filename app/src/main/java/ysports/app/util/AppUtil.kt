package ysports.app.util

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import ysports.app.R

class AppUtil(val context: Context) {

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
            Toast.makeText(context, context.getString(R.string.error_url_load_fail), Toast.LENGTH_LONG).show()
        }
    }

    fun isTablet() : Boolean {
        val widthDp = context.resources.displayMetrics.run { widthPixels / density }
        val heightDp = context.resources.displayMetrics.run { heightPixels / density }
        if (heightDp < widthDp) {
            return heightDp >= 600
        }
        return widthDp >= 600
    }

    fun minScreenWidth() : Int {
        val displayDensity = Resources.getSystem().displayMetrics.density
        val widthDp = context.resources.displayMetrics.run { widthPixels / density }
        val heightDp = context.resources.displayMetrics.run { heightPixels / density }
        if (heightDp < widthDp) {
            return dpToPx(displayDensity, heightDp.toInt())
        }
        return dpToPx(displayDensity, widthDp.toInt())
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }

    @Suppress("unused")
    private fun pxToDp(density: Float, px: Int): Int {
        return (px / density).toInt()
    }
}