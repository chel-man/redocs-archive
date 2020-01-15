package com.redocs.archive.framework

import promise.api.SimplePromise
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class PromiseImpl <T,R> : SimplePromise<T, R>() {

    private var continuation: Continuation<R>? = null

    suspend fun getAsync(): R {
        return suspendCoroutine {
            continuation = it
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