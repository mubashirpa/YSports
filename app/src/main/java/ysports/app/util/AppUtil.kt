package ysports.app.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.firebase.dynamiclinks.ktx.*
import com.google.firebase.ktx.Firebase
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
        if (minScreenWidth() >= 600) {
            return true
        }
        return false
    }

    fun minScreenWidth(): Int {
        return context.resources.configuration.smallestScreenWidthDp
    }

    private fun dpToPx(density: Float, dps: Int): Int {
        return (dps * density + 0.5f).toInt()
    }

    private fun pxToDp(density: Float, px: Int): Int {
        return (px / density).toInt()
    }

    private fun generateDynamicLink(url: String, dynamicLink: (String) -> Unit = {  }) {
        Firebase.dynamicLinks.shortLinkAsync {
            link = Uri.parse(url)
            domainUriPrefix = context.getString(R.string.dynamic_link_url_prefix)
            androidParameters {
                fallbackUrl = Uri.parse(context.getString(R.string.url_download_app))
            }
        }.addOnSuccessListener { (shortLink, flowChartLink) ->
            dynamicLink.invoke("$shortLink")
        }.addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.error_default), Toast.LENGTH_LONG).show()
        }
    }
}