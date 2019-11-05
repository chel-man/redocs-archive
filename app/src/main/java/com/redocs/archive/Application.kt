package com.redocs.archive

import com.redocs.archive.framework.*

class ArchiveApplication : android.app.Application() {

    companion object {
        var filesDir: String? = null
        val documentsDataSource: InMemoryDocumentsDataSource by lazy { InMemoryDocumentsDataSource() }
        val dictionaryDataSource: InMemoryDictionaryDataSource by lazy { InMemoryDictionaryDataSource() }
        val partitionsStructureDataSource: InMemoryPartitionsStructureDataSource by lazy { InMemoryPartitionsStructureDataSource() }
        val filesDataSource: FilesDataSourceStub by lazy { FilesDataSourceStub(filesDir!!) }
        val documentLinksDataSource: InMemoryDocumentLinksDataSource by lazy { InMemoryDocumentLinksDataSource() }
    }
}