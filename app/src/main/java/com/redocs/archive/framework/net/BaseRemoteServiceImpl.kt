package com.redocs.archive.framework.net

import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.ui.events.NetworkStateResponceEvent

abstract class BaseRemoteServiceImpl(
    private var connected: Boolean
) : EventBusSubscriber
{
    init {
        EventBus.subscribe(this, NetworkStateResponceEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is NetworkStateResponceEvent -> connected = evt.data
        }
    }

    protected suspend fun<T> prepareCall(
        clazz: Class<T>,
        url: String
    ): T {

        if(!connected) throw throw Exception("NETWORK NOT CONNECTED")
        RemoteServiceProxyFactory.log = true
        return RemoteServiceProxyFactory
            .create(clazz, url )
    }


}