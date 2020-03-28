package com.redocs.archive

import android.util.Log
import com.redocs.archive.data.links.DocumentLinksDataSource
import com.redocs.archive.data.partitions.PartitionsStructureDataSource
import com.redocs.archive.data.service.SecurityService
import com.redocs.archive.framework.*
import com.redocs.archive.framework.net.RemoteServiceProxyFactory
import com.redocs.archive.ui.events.NetworkStateChangedEvent
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class ArchiveApplication : android.app.Application(), EventBusSubscriber {

    override fun onCreate() {
        super.onCreate()
        EventBus.subscribe(this, NetworkStateChangedEvent::class.java)
    }

    override fun onTerminate() {
        super.onTerminate()
        RemoteServiceProxyFactory.destroy()
        EventBus.unsubscribe(this)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is NetworkStateChangedEvent -> isNetworkConnected = evt.data
        }
    }

    companion object {
        private var isNetworkConnected: Boolean = false
        lateinit var baseUrl: String
        var filesDir: String? = null
        val documentsDataSource: InMemoryDocumentsDataSource by lazy { InMemoryDocumentsDataSource() }
        val dictionaryDataSource: InMemoryDictionaryDataSource by lazy { InMemoryDictionaryDataSource() }
        val partitionsStructureDataSource: PartitionsStructureDataSource by lazy {
                    PartitionStructureDataSourceImpl(
                        "$baseUrl/containers", isNetworkConnected) }
        val filesDataSource: FilesDataSourceStub by lazy { FilesDataSourceStub(filesDir!!) }
        val documentLinksDataSource: DocumentLinksDataSource by lazy { InMemoryDocumentLinksDataSource() }
        val securityService: SecurityService by lazy {
            SecurityServiceImpl("${baseUrl}/security/", isNetworkConnected) }

        fun setup(){
            CookieHandler.setDefault(
                CookieManager(null, CookiePolicy.ACCEPT_ALL))
            Log.d("#APP","CookieManager OK")
        }
    }
}