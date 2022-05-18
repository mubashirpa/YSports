package ysports.app.util

import android.content.Context
import android.webkit.WebResourceResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

object AdBlocker {

    private var AD_HOSTS: MutableSet<String> = HashSet()

    fun init(context: Context) {
        Thread {
            try {
                loadFromAssets(context)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun isAd(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl != null) {
            return isAdHost(httpUrl.host)
        }
        return false
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }

    @Throws(IOException::class)
    private fun loadFromAssets(context: Context) {
        val stream: InputStream = context.assets.open("ad-servers.txt")
        val buffer: BufferedSource = stream.source().buffer()
        val line: String? = buffer.readUtf8Line()
        while (line != null) {
            AD_HOSTS.add(line)
        }
        buffer.close()
        stream.close()
    }

    private fun isAdHost(host: String): Boolean {
        if (host.isEmpty()) {
            return false
        }
        val index = host.indexOf(".")
        return index >= 0 && (AD_HOSTS.contains(host) ||
                index + 1 < host.length && isAdHost(host.substring(index + 1)))
    }
}