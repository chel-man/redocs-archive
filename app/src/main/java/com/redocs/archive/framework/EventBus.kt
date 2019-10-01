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

    private fun send(evt: Event<*>){
        val l=subscribers["${evt::class.java.canonicalName}"]
        if(!l.isNullOrEmpty()) {
            for( s in l) {
                s.onEvent(evt)
            }
            evt.succes?.invoke()
        }
    }

    private fun publish(evt: Event<*>) {
        //CoroutineScope(Dispatchers.Default).launch {
            val l=subscribers["${evt::class.java.canonicalName}"]
            if(!l.isNullOrEmpty()) {
                val h = Handler()
                for( s in l) {
                    h.post{
                        s.onEvent(evt)}
                }
                h.post{
                    evt.succes?.invoke()}
            }
        //}
    }

    abstract class Event<T>(val data: T, val succes: (() -> Unit)? = null)
}

interface EventBusSubscriber {
    fun onEvent(evt: EventBus.Event<*>)
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