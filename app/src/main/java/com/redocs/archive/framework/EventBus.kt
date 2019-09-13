package com.redocs.archive.framework

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass


class EventBus {

    lateinit var scope: CoroutineScope

    private val subscribers: MutableMap<String, MutableList<EventBusSubscriber>> = mutableMapOf()

    private constructor()

    companion object {
        private val eb = EventBus()

        fun subscribe(subscriber: EventBusSubscriber, vararg evts: KClass<out Event<*>> ) {
            eb.subscribe(subscriber, *evts)
        }

        fun publish(msg: Event<*>) {
            eb.publish(msg)
        }

        fun unsubscribe(subscriber: EventBusSubscriber) {
            eb.unsubscribe(subscriber)
        }
    }

    private fun subscribe(subscriber: EventBusSubscriber, vararg evts: KClass<out Event<*>>) {

        for(evt in evts) {
            val clazz = evt.qualifiedName as String
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
        scope.launch(Dispatchers.Default) {
            val l=subscribers["${evt::class}"]
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

fun EventBusSubscriber.subscribe(vararg evts: KClass<out EventBus.Event<*>>) {
    EventBus.subscribe(this, *evts)
}

fun EventBusSubscriber.unsubscribe() {
    EventBus.unsubscribe(this)
}