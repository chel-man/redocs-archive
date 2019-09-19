package com.redocs.archive.framework

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class EventBus {

    //lateinit var scope: CoroutineScope

    private val subscribers: MutableMap<String, MutableList<EventBusSubscriber>> = mutableMapOf()

    private constructor()

    companion object {
        private val eb = EventBus()

        fun subscribe(subscriber: EventBusSubscriber, vararg evts: Class<out Event<*>> ) {
            eb.subscribe(subscriber, *evts)
        }

        fun publish(msg: Event<*>) {
            eb.publish(msg)
        }

        fun unsubscribe(subscriber: EventBusSubscriber) {
            eb.unsubscribe(subscriber)
        }
    }

    private fun subscribe(subscriber: EventBusSubscriber, vararg evts: Class<out Event<*>>) {

        for(evt in evts) {
            val clazz = evt.canonicalName
            val l = subscribers[clazz] ?: mutableListOf()
            synchronized(l) {
                if (!l.contains(subscriber))
                    l.add(subscriber)
            }
            subscribers[clazz] = l
        }
    }

    fun unsubscribe(subscriber: EventBusSubscriber) {
        for(es in subscribers) {
            es.value.remove(subscriber)
        }
    }

    /*private fun send(msg: Event){

    }*/

    private fun publish(evt: Event<*>) {
        CoroutineScope(Dispatchers.Default).launch {
            val l=subscribers["${evt::class.java.canonicalName}"]
            if(!l.isNullOrEmpty()) {
                for( s in l) {
                    s.onEvent(evt)
                }
                evt.succes?.invoke()
            }
        }
    }

    abstract class Event<T>(val data: T, val succes: () -> Unit = {})
}

interface EventBusSubscriber {
    suspend fun onEvent(evt: EventBus.Event<*>)
}

fun EventBusSubscriber.subscribe(vararg evts: Class<out EventBus.Event<*>>) {
    EventBus.subscribe(this, *evts)
}

fun EventBusSubscriber.unsubscribe() {
    EventBus.unsubscribe(this)
}