package com.redocs.archive.framework.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.redocs.archive.framework.EventBus
import com.redocs.archive.ui.events.NetworkStateChangedEvent

class NetworkStateMonitor(private val context: Context) {

    companion object {
        const val WIFI = "wi_fi"
        const val ANY = "any"
        var networkPrefs: String? = ANY
            set(value){
                field = value ?: ANY
            }
    }

    private val delegate: NetworkSateReader

    init {
        delegate =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                StateReader28(context)
            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                StateReader24(context)
            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                StateReader19(context)
            else
                throw Exception("Not supported version")
    }

    fun registerNetworkListener() {
        delegate.registerListener()
    }

    fun unregisterNetworkListener() {
        delegate.unregisterListener()
    }

    private interface NetworkSateReader {
        val isConnected: Boolean
        fun registerListener()
        fun unregisterListener()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class StateReader28(val context: Context) : NetworkSateReader {
        override val isConnected: Boolean
            get() =
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let {
                    isConnected28(it.getNetworkCapabilities(it.activeNetwork))
                }

        override fun registerListener() =
            registerListener24(context)

        override fun unregisterListener() {}

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class StateReader24(val context: Context) : NetworkSateReader {
        override val isConnected: Boolean
            get() = isConnected(context)

        override fun registerListener() = registerListener24(context)

        override fun unregisterListener() {}

    }

    private class StateReader19(val context: Context) : NetworkSateReader {
        override val isConnected: Boolean
            get() = isConnected(context)

        private val bc = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                stateChanged(isConnected)
            }
        }

        override fun registerListener() {
            context.registerReceiver(
                bc,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }

        override fun unregisterListener() = context.unregisterReceiver(bc)

    }

}

private fun stateChanged(connected: Boolean){
    EventBus.publish(NetworkStateChangedEvent(connected))
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun stateChanged(context: Context, network: Network) =
    EventBus.publish(NetworkStateChangedEvent(
        isConnected(
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .getNetworkInfo(network))))

private fun isConnected(context: Context) =
    isConnected(
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .activeNetworkInfo)

private fun isConnected(ni: NetworkInfo?) =
    ni?.let {
        it.isConnected && (
                (NetworkStateMonitor.WIFI == NetworkStateMonitor.networkPrefs &&
                        it.type == ConnectivityManager.TYPE_WIFI) ||
                        NetworkStateMonitor.ANY == NetworkStateMonitor.networkPrefs)
    } ?: false

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun isConnected28(ncaps: NetworkCapabilities) =
    ncaps.let {
        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            NetworkStateMonitor.WIFI == NetworkStateMonitor.networkPrefs &&
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)  ||
                NetworkStateMonitor.ANY == NetworkStateMonitor.networkPrefs
    }

@RequiresApi(Build.VERSION_CODES.N)
private fun registerListener24(context: Context) =
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        .registerDefaultNetworkCallback(
            /*NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                /*.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)*/
                .build(),*/
            object:ConnectivityManager.NetworkCallback(){
                override fun onAvailable(network: Network?) {
                    network?.run {
                        stateChanged(context,network)}
                }

                override fun onUnavailable() {
                    stateChanged(false)
                }
            })
