package com.redocs.archive

import com.redocs.archive.data.links.DocumentLinksDataSource
import com.redocs.archive.data.partitions.PartitionsStructureDataSource
import com.redocs.archive.domain.security.SecurityService
import com.redocs.archive.framework.*

class ArchiveApplication : android.app.Application() {

    companion object {
        lateinit var baseUrl: String
        var filesDir: String? = null
        val documentsDataSource: InMemoryDocumentsDataSource by lazy { InMemoryDocumentsDataSource() }
        val dictionaryDataSource: InMemoryDictionaryDataSource by lazy { InMemoryDictionaryDataSource() }
        val partitionsStructureDataSource: PartitionsStructureDataSource by lazy {
                    PartitionStructureDataSourceImpl("$baseUrl/containers") }
        val filesDataSource: FilesDataSourceStub by lazy { FilesDataSourceStub(filesDir!!) }
        val documentLinksDataSource: DocumentLinksDataSource by lazy { InMemoryDocumentLinksDataSource() }
        val securityService: SecurityService by lazy { InMemorySecurityService() }
    }
}