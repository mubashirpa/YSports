package ysports.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkUtil {

    fun isOnline(context: Context): Boolean {
        var online = false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        if (network != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    online = true
                }
            }
        }
        return online
    }

    fun wifiConnected(context: Context): Boolean {
        var connected = false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network: Network? = connectivityManager.activeNetwork
        if (network != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                connected = true
            }
        }
        return connected
    }
}