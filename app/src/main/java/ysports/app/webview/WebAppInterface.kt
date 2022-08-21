package ysports.app.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.widget.Toast
import ysports.app.PlayerChooserActivity
import ysports.app.YouTubePlayerActivity
import ysports.app.player.PlayerUtil

class WebAppInterface(
    val context: Context,
    val activity: Activity
) {

    @JavascriptInterface
    fun finish() {
        activity.finish()
    }

    @JavascriptInterface
    fun toast(message: String?) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    fun play(url: String?, title: String?) {
        if (url != null) {
            if (url.startsWith("https://youtu.be/")) {
                val intent = Intent(context, YouTubePlayerActivity::class.java).apply {
                    putExtra("VIDEO_URL", url)
                }
                context.startActivity(intent)
            } else PlayerUtil().loadPlayer(context, Uri.parse(url), title, true)
        }
    }

    @JavascriptInterface
    fun media(json: String?) {
        val intent = Intent(context, PlayerChooserActivity::class.java).apply {
            putExtra("JSON_URL", json)
        }
        context.startActivity(intent)
    }

    @JavascriptInterface
    fun changeStatusBarColor(light: Boolean, red: Int, green: Int, blue: Int) {
        activity.runOnUiThread {
            setStatusBarColor(light, red, green, blue)
        }
    }

    private fun setStatusBarColor(light: Boolean, red: Int, green: Int, blue: Int) {
        val window = activity.window
        window?.apply {
            statusBarColor = Color.rgb(red, green, blue)
            if (light) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    controller?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else

                    //Deprecated in Api level 30
                    addFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    controller?.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else

                    //Deprecated in Api level 30
                    clearFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

            }
        }
    }
}