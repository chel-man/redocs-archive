package com.redocs.archive.framework

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import promise.api.SimplePromise
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class PromiseImpl <T,R> : SimplePromise<T, R>() {

    private var continuation: Continuation<R>? = null

    protected open suspend fun resolveAsync(){}

    suspend fun wait(): R {
        if(isDone || isCancelled)
            return get()

        return coroutineScope {
            async{
                resolveAsync()
            }
            val r: R = suspendCoroutine {
                continuation = it
            }
            r
        }
    }

    override fun set(result: R) {
        super.set(result)
        continuation?.resume(result)
    }

    override fun setException(ex: Exception?) {
        super.setException(ex)
        continuation?.resumeWithException(ex as Throwable)
    }
}