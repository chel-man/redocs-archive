package com.redocs.archive

import android.util.Log
import com.redocs.archive.data.links.DocumentLinksDataSource
import com.redocs.archive.data.partitions.PartitionsStructureDataSource
import com.redocs.archive.data.service.SecurityService
import com.redocs.archive.framework.*
import java.net.CookieHandler
import java.net.CookieManager

class ArchiveApplication : android.app.Application() {

    companion object {
        var isNetworkConnected: Boolean = false
        lateinit var baseUrl: String
        var filesDir: String? = null
        val documentsDataSource: InMemoryDocumentsDataSource by lazy { InMemoryDocumentsDataSource() }
        val dictionaryDataSource: InMemoryDictionaryDataSource by lazy { InMemoryDictionaryDataSource() }
        val partitionsStructureDataSource: PartitionsStructureDataSource by lazy {
                    PartitionStructureDataSourceImpl("$baseUrl/containers") }
        val filesDataSource: FilesDataSourceStub by lazy { FilesDataSourceStub(filesDir!!) }
        val documentLinksDataSource: DocumentLinksDataSource by lazy { InMemoryDocumentLinksDataSource() }
        val securityService: SecurityService by lazy {
            SecurityServiceImpl("${baseUrl}/security/", isNetworkConnected) }

        fun setup(){
            Log.d("#APP","CookieManager OK")
            CookieManager.setDefault(
                CookieHandler.getDefault())
        }
    }
}