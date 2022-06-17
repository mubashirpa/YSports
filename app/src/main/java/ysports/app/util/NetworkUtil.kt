package ysports.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

class NetworkUtil {

    fun isOnline(context: Context): Boolean {
        var online = false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        online = true
                    }
                }
            }
        } else {

            //Deprecated in Api level 23
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null) {
                online = networkInfo.isConnected
            }
        }
        return online
    }

    fun wifiConnected(context: Context): Boolean {
        var connected = false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network: Network? = connectivityManager.activeNetwork
            if (network != null) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    connected = true
                }
            }
        } else {

            //Deprecated in Api level 23
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo?.type == ConnectivityManager.TYPE_WIFI) {
                connected = networkInfo.isConnected
            }
        }
        return connected
    }
}