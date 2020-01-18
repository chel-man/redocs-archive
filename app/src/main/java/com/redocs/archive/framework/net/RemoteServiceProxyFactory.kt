package com.redocs.archive.framework.net

import android.util.Log
import com.redocs.archive.framework.PromiseImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import promise.api.Promise
import remote.service.api.ServiceCallInfo
import remote.service.api.SimpleDataEnvelop
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
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation

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

    suspend inline fun <reified T> create(url: String) =
        create(T::class.java, url, timeout)

    suspend fun <T> create(clazz: Class<T>, url: String, tmout: Int): T = withContext(Dispatchers.IO) {

        val scope = this

        Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), object : InvocationHandler {

            @Throws(Throwable::class)
            override fun invoke(proxy: Any, method: Method, _args: Array<*>): Any {

                try {
                    //HttpURLConnection conn;
                    val methodName = method.name
                    if (methodName == "toString")
                        throw NoSuchMethodException("Вызов метода toString !!!")

                    val paramTypes = fixParamTypes(method.parameterTypes)
                    val args = fixArgs(_args,paramTypes.size)
                    writelog("Remote: invoke [$methodName] ${paramTypes.joinToString(",")}")
                    var res: Any? = null
                    val rt = method.returnType
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
                    if (rt is Promise<*, *>) {

                        val promise = object : PromiseImpl<Any, Any>() {

                            private var submitted = false

                            private fun submit() {
                                if (!submitted) {
                                    val promise = this
                                    scope.async {
                                        writelog("RProxy: submitCallRemoteServiceTask");
                                        submitCallRemoteServiceTask(
                                            promise, url, methodName, args, paramTypes,
                                            tmout, retries
                                        )
                                    }
                                    submitted = true;
                                }
                            }

                            override fun resolve(): Promise<Any, Any> {
                                submit()
                                return this;
                            }

                            override fun get(): Any {
                                submit()
                                try {
                                    return super.get()
                                } catch (e: Exception) {
                                    throw unpackException(e)
                                }
                            }

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

    private fun fixArgs(args: Array<*>, size: Int): Array<*> {

        if(args.size > size)
            return args.sliceArray(0 until size)
        return args
    }

    private fun fixParamTypes(params: Array<Class<*>>): Array<Class<*>> {

        for((i, pt) in params.withIndex()){
            Log.d("#FIX","$pt")
            if("$pt" ==  "interface kotlin.coroutines.Continuation") {
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
    }

    private static ExecutorService executor;

    private static ThreadFactory ff=new ThreadFactory(){
            @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
              }};

    private static synchronized ExecutorService getExecutor(){
        if(executor==null){
            if(mt)
                executor=Executors.newCachedThreadPool(ff);
            else
                executor=Executors.newSingleThreadExecutor(ff);
        }

        return executor;
    }*/

    private fun callRemoteService(url: String, methodName: String, args: Array<*>,
                                    paramTypes: Array<Class<*>>, timeout: Int, retries: Int
    ): Pair<ObjectInputStream, ObjectOutputStream> {

        var tries = retries
        var rurl=url

        checkNetworkState()
        if(log){
            var sargs = ""
            for(p in args)
                sargs+= (if(sargs.isEmpty()) "" else ",")+p
            writelog("RProxy:callRemoteService $url ["+methodName+"] "+sargs);
            //rurl+=(if(url.endsWith("/")) "" else "/")+"log";
        }

        while(true){

            var ints = getTimeStamp()
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

    private fun checkNetworkState() {

    }

    private val df = SimpleDateFormat("dd.MM HH:mm:ss")

    private fun getTimeStamp(): String {
        return df.format(Date())
    }

    private fun submitCallRemoteServiceTask(
        promise: Promise<Any, Any>,
        url: String,
        methodName: String,
        args: Array<*>,
        paramTypes: Array<Class<*>>,
        timeout: Int, retries: Int
    ): Unit {
        try {
            writelog("RProxy:Executor starting task ...")
            if (promise.isDone || promise.isCancelled) {
                writelog("RProxy:Executor promise done or cancelled ...")
                return
            }
            var lres: Any?
            var tries = retries
            while (true) {
                lateinit var ins: ObjectInputStream
                lateinit var out: ObjectOutputStream
                try {
                    try {
                        //long ltime=System.currentTimeMillis();
                        callRemoteService(url, methodName, args, paramTypes, timeout, 0).apply {
                            ins = first
                            out = second
                        }
                        //System.out.println(">"+methodName+":"+(System.currentTimeMillis()-ltime));
                        if (retries != tries)
                            writelog("retrying $url/$methodName $retries/$tries OK")
                    } catch (e: IOException) {
                        if (tries > 0) {
                            writelog("retrying $url/$methodName")
                            tries--
                            continue
                        }
                        e.printStackTrace();
                        setPromiseException(promise, e)
                        return
                    } catch (ex: Throwable) {
                        if (!isAppException(ex) && tries > 0) {
                            writelog("retrying $url/$methodName")
                            tries--
                            continue
                        }
                        ex.printStackTrace();
                        setPromiseException(promise,/*out,ins,*/Exception(ex))
                        return
                    }

                    val timeRef = AtomicLong(0)
                    try {
                        var totalTime = 0L
                        /*val cins = ins as CustomObjectInputStream
                        if(logDataTransmitTime)
                            cins.addDataReadyListener(object:ActionListener() {

                                    override fun actionPerformed(e: ActionEvent) {
                                        timeRef.set(System.currentTimeMillis());
                                    }
                                })*/

                        while (true) {
                            writelog("RProxy:reading object ...")
                            //cins.setCallListener()
                            val env = ins.readObject() as SimpleDataEnvelop

                            if (logDataTransmitTime)
                                totalTime += System.currentTimeMillis() - timeRef.get()

                            writelog("RProxy:reading object OK [${env.getAnnotation().name}]")
                            val data = env.getData()
                            if (env.getAnnotation() == SimpleDataEnvelop.DataAnnotation.Error) {
                                promise.setException(data as Exception)
                                return
                            } else if (env.getAnnotation() == SimpleDataEnvelop.DataAnnotation.Result) {
                                lres = data
                                if (totalTime > 0)
                                    writelog("RProxy: [$methodName] data transmit time $totalTime,${LogType.DataTransfer}")
                                break
                            } else if (env.getAnnotation() == SimpleDataEnvelop.DataAnnotation.Ping) {
                                writelog("=====> PING RECIVED")
                                continue
                            }

                            promise.setPartial(data)
                        }
                        writelog("RProxy:setting result ...")
                        promise.set(lres)
                        writelog("RProxy:setting result OK");
                        if (retries != tries)
                            writelog("retrying DATA $url/$methodName $retries/$tries OK")
                        return
                        /*}catch(EOFException eof){
                            System.out.println("========= EOF ================");*/
                    } catch (ce: CancellationException) {
                        return;
                    } catch (ice: InvalidClassException) {
                        setPromiseException(promise, /*out, ins,*/ ice);
                        return;
                    } catch (e: IOException) {

                        if (tries > 0) {
                            writelog("retrying DATA $url/$methodName")
                            tries--
                            continue
                        }
                        e.printStackTrace();
                        setPromiseException(promise, /*out, ins,*/ e);
                        return;
                    } catch (ex: Exception) {

                        /*if(!isAppException(ex) && tries>0){
                            System.out.println("retrying "+url+"/"+methodName);
                            tries--;
                            continue;
                        }*/
                        ex.printStackTrace();
                        setPromiseException(promise, /*out, ins,*/ ex);
                        return
                    }
                } finally {
                    try {
                        out.close();
                    } catch (e: Throwable) {}
                    try {
                        ins.close();
                    } catch (e: Throwable) {}
                }
            }
        } finally {
            writelog("RProxy:Executor task copleted");
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
        /*if(executor!=null){
            ExecutorService es = executor;
            executor=null;
            System.out.println("RemoteServiceProxy executor shutting down ...");
            try {
                es.shutdownNow();
                es.awaitTermination(3, TimeUnit.SECONDS);
                System.out.println("OK");
            } catch (InterruptedException ex) {
                System.out.println("Interrupted");
            }catch(Exception e){
                System.out.println("RemoteServiceProxy Error");
                e.printStackTrace();
            }
        }*/
    }

    private fun isAppException(e: Throwable): Boolean {
        return true
    }

    private fun writelog(s: String): Unit {
        writelog(s, LogType.Message);
    }

    private fun writelog(s: String, type: LogType) {
        if(log && type==LogType.Message)
            Log.d("#RemoteDBG",s)
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