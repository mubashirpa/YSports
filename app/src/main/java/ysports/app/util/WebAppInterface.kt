package ysports.app.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.widget.Toast
import ysports.app.player.PlayerUtil

class WebAppInterface {

    private var context: Context? = null
    private var activity: Activity? = null
    private var window: Window? = null

    constructor(context: Context, activity: Activity) {
        this.context = context
        this.activity = activity
    }

    constructor(context: Context, activity: Activity, window: Window) {
        this.context = context
        this.activity = activity
        this.window = window
    }

    @JavascriptInterface
    fun exitActivity() {
        activity?.finish()
    }

    @JavascriptInterface
    fun showToast(message: String?) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun loadPlayer(url: String?) {
        if (url != null) {
            PlayerUtil().loadPlayer(context!!, Uri.parse(url), true)
        }
    }

    @JavascriptInterface
    fun loadPlayerYT(url: String?) {
        if (url != null) {
            YouTubePlay(context!!).playVideo(url)
        }
    }

    @JavascriptInterface
    fun changeStatusBarColor(light: Boolean, red: Int, green: Int, blue: Int) {
        activity?.runOnUiThread {
            setStatusBarColor(light, red, green, blue)
        }
    }

    private fun setStatusBarColor(light: Boolean, red: Int, green: Int, blue: Int) {
        window?.apply {
            statusBarColor = Color.rgb(red, green, blue)
            if (light) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window!!.insetsController
                    controller?.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    //Deprecated in Api level 30
                    addFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window!!.insetsController
                    controller?.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    //Deprecated in Api level 30
                    clearFlags(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

                }
            }
        }
    }
}