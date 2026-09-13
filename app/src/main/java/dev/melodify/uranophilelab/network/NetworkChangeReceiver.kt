package dev.melodify.uranophilelab.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

class NetworkChangeReceiver(private val listener: NetworkStatusListener) : BroadcastReceiver() {
    // Callback interface to notify when the internet is connected or disconnected
    interface NetworkStatusListener {
        fun onNetworkConnected()

        fun onNetworkDisconnected()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetworkInfo

        if (activeNetwork != null && activeNetwork.isConnected) {
            listener.onNetworkConnected()
        } else {
            listener.onNetworkDisconnected()
        }
    }

    companion object {
        fun registerReceiver(context: Context?, receiver: NetworkChangeReceiver?) {
            if (context == null || receiver == null) return
            try {
                val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                context.registerReceiver(receiver, filter)
            } catch (e: Exception) {
                // Ignore if already registered
            }
        }

        fun unregisterReceiver(context: Context?, receiver: NetworkChangeReceiver?) {
            if (context == null || receiver == null) return
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver not registered
            }
        }
    }
}
