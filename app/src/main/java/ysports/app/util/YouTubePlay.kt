package ysports.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.SparseArray
import android.widget.Toast
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ysports.app.player.PlayerUtil

@SuppressLint("StaticFieldLeak")
class YouTubePlay(var context: Context) {

    fun playVideo(url: String) {
        if (url.isNotEmpty() && url.startsWith("https://youtu.be/")) {
            Toast.makeText(context, "Please wait while we loading URLs", Toast.LENGTH_LONG).show()
            object : YouTubeExtractor(context) {
                override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, vMeta: VideoMeta?) {
                    if (ytFiles != null) {
                        val videoList = ArrayList<String>()
                        val videoURL = ArrayList<String>()

                        for (i in 0 until ytFiles.size()) {
                            val tag = ytFiles.keyAt(i)
                            val file = ytFiles[tag]

                            if (file.format.height == -1 || file.format.height >= 360) {
                                var title = if (file.format.height == -1) "Audio " + file.format.audioBitrate.toString() + " kbit/s"
                                else file.format.height.toString() + "p"
                                title += if (file.format.isDashContainer) " dash" else ""
                                videoList.add(title)
                                videoURL.add(ytFiles[tag].url)
                            }
                        }

                        val videoItems = videoList.toArray(arrayOfNulls<CharSequence>(videoList.size))
                        MaterialAlertDialogBuilder(context)
                            .setTitle(vMeta!!.title ?: "")
                            .setItems(videoItems) { _, position ->
                                val playerUtil = PlayerUtil()
                                playerUtil.loadPlayer(context, Uri.parse(videoURL[position]), null, true)
                            }
                            .show()
                    } else {
                        Toast.makeText(context, "An error occurred", Toast.LENGTH_LONG).show()
                    }
                }
            }.extract(url)
        } else Toast.makeText(context, "An error occurred", Toast.LENGTH_LONG).show()
    }
}