package com.redocs.archive.framework.net

import android.util.Log
import com.redocs.archive.framework.PromiseImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import promise.api.Promise
import remote.service.api.ServiceCallInfo
import remote.service.api.SimpleDataEnvelop
import remote.service.api.SimpleDataEnvelop.DataAnnotation.*
import java.io.IOException
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.SocketTimeoutException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

object RemoteServiceProxyFactory {

    var timeout=50000
    var retries=0
    var log=false

    //private var mt = false
    private var logDataTransmitTime=false

    /*fun setAsyncParallel(v: Boolean)
    {
        executor = null
        mt = v
    }*/

    inline fun <reified T> create(url: String) =
        create(T::class.java, url, timeout)

    fun <T> create(clazz: Class<T>, url: String) =
        create(clazz, url, timeout)

    fun <T> create(clazz: Class<T>, url: String, tmout: Int): T {

        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), object : InvocationHandler {

            @Throws(Throwable::class)
            override fun invoke(proxy: Any, method: Method, _args: Array<*>): Any {

                try {
                    //HttpURLConnection conn;
                    val methodName = method.name
                    if (methodName == "toString")
                        throw NoSuchMethodException("Вызов метода toString !!!")

                    val paramTypes = fixParamTypes(method.parameterTypes)
                    val (continuation,args) = fixArgs(_args,paramTypes.size)
                    continuation?.run {
                        writelog("==> Suspend method call detected")
                    }
                    val rt = method.kotlinFunction?.javaMethod?.returnType
                    writelog("Remote: invoke [$methodName] ${paramTypes.joinToString(",")} : $rt")
                    var res: Any? = null
                    /*if(rt.equals(DeferredPromise.class)){
                        final DeferredPromise promise=(DeferredPromise) new DeferredBlockingPromiseImpl(){

                            @Override
                            public Promise resolve(Object[] params, Class[] types) {
                                final Object[] __args=Arrays.copyOf(_args,_args.length+params.length);
                                final Class<?>[] _paramTypes=Arrays.copyOf(paramTypes,_args.length+params.length);
                                for(int i=_args.length;i<_args.length+params.length;i++){
                                    __args[i]=params[i-_args.length];
                                    _paramTypes[i]=types[i-_args.length];
                                }
                                submitCallRemoteServiceTask(this,url,methodName,__args,_paramTypes,timeout,retries);
                                return this;
                            }

                        };
                        res=promise;
                    }
                    else*/
                    if (rt!!.name == Promise::class.java.name) {
                        writelog("PROMISE return type detected")
                        val promise = object : PromiseImpl<Any,Any>() {

                            private var submitted = false

                            private suspend fun submit() {
                                val promise = this
                                if (!submitted) {
                                    /*coroutineScope {
                                        async {*/
                                            writelog("RProxy: submitCallRemoteServiceTask $this");
                                            submitCallRemoteServiceTask(
                                                promise, url, methodName, args, paramTypes,
                                                tmout, retries
                                            )
                                            //(continuation as Continuation<Promise<*,*>>)?.resume(promise)
                                        /*}
                                    }*/
                                    submitted = true;
                                }
                            }

                            override suspend fun resolveAsync(): Unit = submit()

                            /*override fun get(): Any {
                                submit()
                                try {
                                    return super.get()
                                } catch (e: Exception) {
                                    throw unpackException(e)
                                }
                            }*/

                        };
                        res = promise
                    } else {
//long ltime=System.currentTimeMillis();
                        //try{
                        val (ins, out) = callRemoteService(
                            url, methodName, args, paramTypes, tmout, retries
                        )

                        ins.use {
                            out.use {
                                res = ins.readObject()
                                if (res is Exception)
                                    throw res as Exception
                            }
                        }
                        /*}finally{
                            try{
                                out.close();
                            }catch(Exception e){}
                            try{
                                in.close();
                            }catch(Exception e){}
                        }*/
//System.out.println(methodName+":"+(System.currentTimeMillis()-ltime));
                    }
                    return res as Any
                } catch (nme: NoSuchMethodException) {
                    nme.printStackTrace()
                    throw nme
                } catch (e: Throwable) {
                    throw unwrapException(e)
                }
            }

        }) as T
    }

    private fun fixArgs(args: Array<*>, size: Int): Pair<Continuation<*>?,Array<*>> {

        if(args.size > size)
            return args.last() as Continuation<*> to  args.sliceArray(0 until size)
        return null to args
    }

    private fun fixParamTypes(params: Array<Class<*>>): Array<Class<*>> {

        for((i, pt) in params.withIndex()){
            if(pt.name == Continuation::class.java.name) {
                writelog("==> Continuation param detected")
                return params.sliceArray(0 until i)
            }
        }

        return params
    }

    /*private fun unwrapException(e: Exception): Exception{
        var c = e
        while(true){
            val cc = c.cause as? Exception? ?: break
            c=cc
        }

        return c
    }*/

    private fun unwrapException(e: Throwable): Throwable{
        var c = e
        while(true){
            val cc = c.cause ?: break
            c=cc
        }

        return c
    }

    /*private static Object fromByteArray(byte[] ba) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(ba);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        ois.close();
        bis.close();
        return obj;
    }*/

    private val executor =  lazy {
        Executors.newCachedThreadPool { r ->
            Thread(r).apply {
                isDaemon = true
            }
        }
    }

    private fun callRemoteService(url: String, methodName: String, args: Array<*>,
                                    paramTypes: Array<Class<*>>, timeout: Int, retries: Int
    ): Pair<ObjectInputStream, ObjectOutputStream>
    {
        var tries = retries
        var rurl=url

        if(log){
            var sargs = ""
            for(p in args)
                sargs+= (if(sargs.isEmpty()) "" else ",")+p
            writelog("RProxy:callRemoteService $url ["+methodName+"] "+sargs);
            //rurl+=(if(url.endsWith("/")) "" else "/")+"log";
        }

        while(true){

            val ints = getTimeStamp()
            try{
                val serviceUrl = URL(rurl)
                val conn = serviceUrl.openConnection().apply {
                    readTimeout = timeout
                    doInput = true
                    doOutput = true
                    useCaches = false
                }
                val out = ObjectOutputStream(conn.outputStream)

                out.writeObject(
                    ServiceCallInfo(timeout).apply {
                        this.methodName = methodName
                        paramClasses = paramTypes
                        params = args
                    }
                )

                out.flush()
                if(retries!=tries)
                    writelog("retrying $rurl/$methodName $retries/$tries OK");
                writelog("RProxy:callRemoteService OK");
                return ObjectInputStream(conn.getInputStream()) to out
                //CustomObjectInputStream(CustomInputStream(conn.getInputStream())) to out
            }catch(e: IOException){
                if(e is SocketTimeoutException)
                    Log.e("RemoteService","$ints - ${getTimeStamp()}")
                if(tries==0)
                    throw e;
            }catch(e: Throwable){
                if(isAppException(e) || tries==0)
                    throw e
            }
            //conn.disconnect();
            writelog("call retrying $rurl/$methodName ($tries)")
            tries--
        }
    }

    private val df = SimpleDateFormat("dd.MM HH:mm:ss")

    private fun getTimeStamp(): String {
        return df.format(Date())
    }

    private suspend fun submitCallRemoteServiceTask(
        promise: Promise<Any, Any>,
        url: String,
        methodName: String,
        args: Array<*>,
        paramTypes: Array<Class<*>>,
        timeout: Int, retries: Int
    ): Unit /*= coroutineScope*/ {
        try {
            writelog("RProxy:Executor starting task ...")
            if (promise.isDone || promise.isCancelled) {
                writelog("RProxy:Executor promise done or cancelled ...")
                return
            }
            var tries = retries
            while (true) {
                /*lateinit var ins: ObjectInputStream
                lateinit var out: ObjectOutputStream*/
                //yield()
                //try {
                    try {
                        //long ltime=System.currentTimeMillis();
                        val (ins,out) =
                            callRemoteService(url, methodName, args, paramTypes, timeout, 0)
                        //System.out.println(">"+methodName+":"+(System.currentTimeMillis()-ltime));
                        if (retries != tries)
                            writelog("retrying $url/$methodName $retries/$tries OK")
                        ins.use{
                            out.use {
                                readData(ins,promise,url,methodName)
                            }
                        }
                    } catch (e: IOException) {
                        if (tries > 0) {
                            writelog("retrying $url/$methodName")
                            tries--
                            continue
                        }
                        //e.printStackTrace();
                        setPromiseException(promise, e)
                        return
                    } catch (ex: Throwable) {
                        if (!isAppException(ex) && tries > 0) {
                            writelog("retrying $url/$methodName")
                            tries--
                            continue
                        }
                        //ex.printStackTrace();
                        setPromiseException(promise,/*out,ins,*/Exception(ex))
                        return
                    }

                /*} finally {
                    try {
                        out.close();
                    } catch (e: Throwable) {}
                    try {
                        ins.close();
                    } catch (e: Throwable) {}
                }*/
            }
        } finally {
            writelog("RProxy:Executor task copleted");
        }
    }

    private suspend fun readData(
        ins: ObjectInputStream,
        promise: Promise<Any,Any>,
        url: String,
        methodName: String
    ) {
        val timeRef = AtomicLong(0)
        //try {
            var totalTime = 0L
            var tries = retries
            var lres: Any?

            loop@ while (true) {
                writelog("RProxy:reading object ...")
                //cins.setCallListener()
                val env = readEnvelop(ins)//ins.readObject() as SimpleDataEnvelop

                if (logDataTransmitTime)
                    totalTime += System.currentTimeMillis() - timeRef.get()

                writelog(s = "RProxy:reading object OK [${env.annotation.name}]")
                val data = env.data
                when(env.annotation) {
                    Error -> {
                        promise.setException(data as Exception)
                        return
                    }
                    Result -> {
                        lres = data
                        if (totalTime > 0)
                            writelog("RProxy: [$methodName] data transmit time $totalTime,${LogType.DataTransfer}")
                        break@loop
                    }
                    Ping -> {
                        writelog("=====> PING RECIVED")
                        continue@loop
                    }
                    else -> Unit
                }
                try {
                    promise.setPartial(data)
                }catch (ce: CancellationException){
                    // Client Side Cancellation
                    //out.writeObject(ce)
                    return
                }
            }
            writelog("RProxy:setting result ...")
            promise.set(lres)
            writelog("RProxy:setting result OK");
            if (retries != tries)
                writelog("retrying DATA $url/$methodName $retries/$tries OK")
            //return
            /*}catch(EOFException eof){
                System.out.println("========= EOF ================");*/
        /*} catch (ce: CancellationException) {
            //SS Cancellation
            setPromiseException(promise, ce)
            return
        } catch (ice: InvalidClassException) {
            setPromiseException(promise, /*out, ins,*/ ice);
            return
        } catch (e: IOException) {

            if (tries > 0) {
                writelog("retrying DATA $url/$methodName")
                tries--
                continue
            }
            //e.printStackTrace();
            setPromiseException(promise, /*out, ins,*/ e);
            return
        } catch (ex: Throwable) {

            writelog("EXCEPTION\n${ex.localizedMessage}")
            setPromiseException(promise, /*out, ins,*/ java.lang.Exception(ex))
            return
        }*/

    }

    private suspend fun readEnvelop(ins: ObjectInputStream): SimpleDataEnvelop =
        suspendCoroutine<SimpleDataEnvelop> {
            executor.value.execute {
                try {
                    it.resume(ins.readObject() as SimpleDataEnvelop)
                }catch (e:Exception){
                    it.resumeWithException(e)
                }
            }
        }

    private fun setPromiseException(
        promise: Promise<Any,Any>,
        /*out: ObjectOutputStream,
        ins: ObjectInputStream,*/
        e: Exception
    ):Unit
    {
        promise.setException(e)
        /*try{
            out.close()
        }catch(ex: Exception){}
        try{
            ins.close();
        }catch(ex: Exception){}*/
    }

    fun destroy(): Unit
    {
        if(executor.isInitialized()){
            executor.value.apply {
                writelog("RemoteServiceProxy executor shutting down ...");
                try {
                    shutdownNow();
                    awaitTermination(3, TimeUnit.SECONDS);
                    writelog("OK");
                } catch (ex: InterruptedException) {
                    writelog("Interrupted");
                } catch (e: Exception) {
                    writelog("RemoteServiceProxy Error");
                }
            }
        }
    }

    private fun isAppException(e: Throwable): Boolean {
        return true
    }

    private fun writelog(s: String): Unit {
        writelog(s, LogType.Message);
    }

    private fun writelog(s: String, type: LogType) {
        if(log && type==LogType.Message)
            Log.d("#NETPROXY",s)
        if(logDataTransmitTime && type == LogType.DataTransfer )
            System.out.println(s);
    }

    /*private class CustomObjectInputStream(
        private val inputStream: CustomInputStream
    ) : ObjectInputStream(inputStream) {

        fun addDataReadyListener(l: ActionListener): Unit{
            inputStream.dataReadyListener = l
        }

        fun setCallListener():Unit {
            inputStream.callListener = true
        }

    }

    private class CustomInputStream(private val ris: InputStream) : InputStream() {

        var dataReadyListener: ActionListener? = null
        var callListener = false

        override fun read(): Int {
            val r = ris.read()
            if(dataReadyListener!=null && callListener){
                callListener=false;
                dataReadyListener.actionPerformed(ActionEvent(this, 0, null));
            }
            return r;
        }

    }*/

    private enum class LogType {
        Message,
        DataTransfer
    }
}