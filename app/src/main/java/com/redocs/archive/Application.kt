package com.redocs.archive

import com.redocs.archive.data.dictionary.DictionaryDataSource
import com.redocs.archive.data.links.DocumentLinksDataSource
import com.redocs.archive.framework.*
import java.util.concurrent.atomic.AtomicReference

class ArchiveApplication : android.app.Application() {

    companion object {
        private var dds: AtomicReference<InMemoryDocumentsDataSource> = AtomicReference()
        private var dcds: AtomicReference<InMemoryDictionaryDataSource> = AtomicReference()
        private var pds: AtomicReference<InMemoryPartitionsStructureDataSource> = AtomicReference()
        private var fds: AtomicReference<InMemoryFilesDataSource> = AtomicReference()
        private var lds: AtomicReference<InMemoryDocumentLinksDataSource> = AtomicReference()

        val dictinaryDataSource : InMemoryDictionaryDataSource get() {
            dcds.compareAndSet(null,InMemoryDictionaryDataSource())
            return dcds.get()
        }
        val partitionsStructureDataSource: InMemoryPartitionsStructureDataSource get() {
            pds.compareAndSet(null,InMemoryPartitionsStructureDataSource())
            return pds.get()

        }
        val documentsDataSource: InMemoryDocumentsDataSource get() {
            dds.compareAndSet(null,InMemoryDocumentsDataSource())
            return dds.get()
        }
        val filesDataSource: InMemoryFilesDataSource get() {
            fds.compareAndSet(null,InMemoryFilesDataSource())
            return fds.get()
        }
        val documentLinksDataSource: DocumentLinksDataSource get() {
            lds.compareAndSet(null,InMemoryDocumentLinksDataSource())
            return lds.get()
        }
    }
}