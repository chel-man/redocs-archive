package com.redocs.archive.framework.net

import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.ui.events.NetworkStateChangedEvent

abstract class BaseRemoteServiceImpl(
    private var connected: Boolean
) : EventBusSubscriber
{
    init {
        EventBus.subscribe(this, NetworkStateChangedEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is NetworkStateChangedEvent -> connected = evt.data
        }
    }

    protected inline fun <reified T> getService(url: String): T =
        getService(T::class.java,url)

    protected fun<T> getService(
        clazz: Class<T>,
        url: String
    ): T
    {

        if(!connected) throw Exception("NETWORK NOT CONNECTED")
        RemoteServiceProxyFactory.log = true
        return RemoteServiceProxyFactory
            .create(clazz, url )
    }


}