package com.redocs.archive.framework

import com.redocs.archive.data.links.DocumentLinksDataSource
import com.redocs.archive.domain.links.DocumentLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InMemoryDocumentLinksDataSource : DocumentLinksDataSource {

    private var links = mutableListOf<DocumentLink>(
        DocumentLink(1,"Link 1",1,4),
        DocumentLink(2,"Link 2",1,1),
        DocumentLink(3,"Link 3",2,3)
    )

    override suspend fun list(documentId: Long) = withContext(Dispatchers.IO) {

        val ls = mutableListOf<DocumentLink>()
        for(l in links){
            if(l.sourceId == documentId)
                ls += l
        }
        ls
    }

}