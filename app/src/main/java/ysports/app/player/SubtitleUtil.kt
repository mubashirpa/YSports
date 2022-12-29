package ysports.app.player

import android.net.Uri
import android.webkit.URLUtil
import com.google.android.exoplayer2.util.MimeTypes
import java.util.*

class SubtitleUtil {

    private val supportedMimeTypesSubtitle = arrayOf(
        MimeTypes.APPLICATION_SUBRIP,
        MimeTypes.TEXT_SSA,
        MimeTypes.TEXT_VTT,
        MimeTypes.APPLICATION_TTML,
        "text/*",
        "application/octet-stream"
    )

    private val supportedExtensionsSubtitle =
        arrayOf("srt", "ssa", "ass", "vtt", "ttml", "dfxp", "xml")

    fun getSubtitleMimeType(uri: Uri): String {
        val path: String? = uri.path
        return if (path == null) {
            MimeTypes.APPLICATION_SUBRIP
        } else if (path.endsWith(".ssa") || path.endsWith(".ass")) {
            MimeTypes.TEXT_SSA
        } else if (path.endsWith(".vtt")) {
            MimeTypes.TEXT_VTT
        } else if (path.endsWith(".ttml") || path.endsWith(".xml") || path.endsWith(".dfxp")) {
            MimeTypes.APPLICATION_TTML
        } else {
            MimeTypes.APPLICATION_SUBRIP
        }
    }

    fun getSubtitleLanguage(uri: Uri): String? {
        val path = uri.path?.lowercase(Locale.getDefault()) ?: return null
        if (path.endsWith(".srt")) {
            val last = path.lastIndexOf(".")
            var prev = last
            for (i in last downTo 0) {
                prev = path.indexOf(".", i)
                if (prev != last) break
            }
            val len = last - prev
            if (len in 2..6) {
                // TODO: Validate lang
                return path.substring(prev + 1, last)
            }
        }
        return null
    }

    fun isSubtitle(uri: Uri?, mimeType: String?): Boolean {
        if (mimeType != null) {
            for (mime in supportedMimeTypesSubtitle) {
                if (mimeType == mime) {
                    return true
                }
            }
            if (mimeType == "text/plain" || mimeType == "text/x-ssa" || mimeType == "application/octet-stream" || mimeType == "application/ass" || mimeType == "application/ssa" || mimeType == "application/vtt") {
                return true
            }
        }
        if (uri != null) {
            if (URLUtil.isNetworkUrl(uri.toString())) {
                var path = uri.path
                if (path != null) {
                    path = path.lowercase(Locale.getDefault())
                    for (extension in supportedExtensionsSubtitle) {
                        if (path.endsWith(".$extension")) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}