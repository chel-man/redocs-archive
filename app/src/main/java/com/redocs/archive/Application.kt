package com.redocs.archive

import com.redocs.archive.framework.InMemoryDocumentsDataSource
import com.redocs.archive.framework.InMemoryFilesDataSource
import com.redocs.archive.framework.InMemoryPartitionsStructureDataSource

class ArchiveApplication : android.app.Application() {

    companion object {
        val partitionsStructureDataSource get() = InMemoryPartitionsStructureDataSource()
        val documentsDataSource get() = InMemoryDocumentsDataSource()
        val filesDataSource: InMemoryFilesDataSource get() = InMemoryFilesDataSource()
    }
}