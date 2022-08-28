package ysports.app.webview

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import ysports.app.PlayerChooserActivity
import ysports.app.R
import ysports.app.YouTubePlayerActivity
import ysports.app.player.PlayerUtil
import ysports.app.util.NotificationUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Suppress("PrivatePropertyName")
class WebAppInterface(
    val context: Context,
    val activity: Activity
) {

    private val TAG = "WebAppInterface"
    private val BASE64_SCHEME = "data:"

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

    @JavascriptInterface
    fun downloadBase64(url: String, fileName: String) {
        if (url.startsWith(BASE64_SCHEME)) {
            activity.runOnUiThread {
                Toast.makeText(context, context.getString(R.string.downloading_file), Toast.LENGTH_LONG).show()
                //val fileType = url.substring(url.indexOf("/") + 1, url.indexOf(";"))
                //val fileName = System.currentTimeMillis().toString() + "." + fileType
                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(path, fileName)

                try {
                    if (!path.exists()) path.mkdirs()
                    if (!file.exists()) file.createNewFile()

                    val base64EncodedString: String = url.substring(url.indexOf(",") + 1)
                    val decodedBytes: ByteArray = Base64.decode(base64EncodedString, Base64.DEFAULT)
                    val outputStream = FileOutputStream(file, false)
                    outputStream.write(decodedBytes)
                    outputStream.close()

                    //Tell the media scanner about the new file so that it is immediately available to the user.
                    MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null) { scan_path, uri ->
                        Log.i(TAG, "Scanned: $scan_path")
                        Log.i(TAG, "Uri: $uri")
                    }

                    //Set notification after download complete and add "click to view" action to that
                    val mimetype = url.substring(url.indexOf(":") + 1, url.indexOf("/"))

                    val channelId: String = context.getString(R.string.default_notification_channel_id)
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        setDataAndType(FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file), "${mimetype}/*")
                    }
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                    val priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        NotificationManager.IMPORTANCE_DEFAULT
                    } else {
                        NotificationCompat.PRIORITY_DEFAULT
                    }
                    NotificationUtil(context).sendNotification(channelId, fileName, context.getString(R.string.downloaded_successfully), priority, pendingIntent)
                    Toast.makeText(context, context.getString(R.string.download_complete), Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    Toast.makeText(context, context.getString(R.string.error_download_failed), Toast.LENGTH_LONG).show()
                }
            }
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