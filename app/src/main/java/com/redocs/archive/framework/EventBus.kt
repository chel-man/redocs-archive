package com.redocs.archive.framework

import android.app.Activity
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
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

        fun send(msg: Event<*>) {
            eb.send(msg)
        }

        suspend fun <T> call(msg: Event<*>): T? {
            return eb.call(msg)
        }

        fun unsubscribe(subscriber: EventBusSubscriber) {
            eb.unsubscribe(subscriber)
        }
    }

    private fun subscribe(subscriber: EventBusSubscriber, vararg evts: Class<out Event<*>>) {

        for(evt in evts) {
            evt.canonicalName?.run {
                val l = subscribers[this] ?: mutableListOf()
                synchronized(l) {
                    if (!l.contains(subscriber))
                        l.add(subscriber)
                }
                subscribers[this] = l
            }
        }
    }

    fun unsubscribe(subscriber: EventBusSubscriber) {
        for(es in subscribers) {
            es.value.remove(subscriber)
        }
    }

    private fun send(evt: Event<*>){
        val l=subscribers["${evt::class.java.canonicalName}"]
        if(!l.isNullOrEmpty()) {
            for( s in l) {
                s.onEvent(evt)
            }
            evt.succes?.invoke()
        }
    }

    private suspend fun <T> call(evt: Event<*>): T? {
        evt::class.java.canonicalName?.run {
            val l = subscribers[this]
            if (!l.isNullOrEmpty()) {
                for (s in l) {
                    (s as? EventBusCallSubscriber)?.apply {
                        return@call onCall(evt) as T?
                    }
                }
            }
        }
        return null
    }

    private fun publish(evt: Event<*>) {
        //CoroutineScope(Dispatchers.Default).launch {
        evt::class.java.canonicalName?.run {
            val l = subscribers[this]
            if (!l.isNullOrEmpty()) {
                val h = Handler()
                for (s in l) {
                    h.post {
                        s.onEvent(evt)
                    }
                }
                h.post {
                    evt.succes?.invoke()
                }
            }
        }
        //}
    }

    abstract class Event<T>(val data: T, val succes: (() -> Unit)? = null)
}

interface EventBusSubscriber {
    fun onEvent(evt: EventBus.Event<*>)
}

interface EventBusCallSubscriber : EventBusSubscriber {
    suspend fun onCall(evt: EventBus.Event<*>): Any?
}

fun EventBusSubscriber.subscribe(vararg evts: Class<out EventBus.Event<*>>) {
    EventBus.subscribe(this, *evts)
    (this as? LifecycleOwner)?.lifecycle?.addObserver(
        object:LifecycleObserver{

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy(){
                unsubscribe()
            }
        })
}

private fun EventBusSubscriber.unsubscribe() {
    EventBus.unsubscribe(this)
}