package com.redocs.archive.data.links

class DocumentLinksRepository(
    private val ds: DocumentLinksDataSource
)
{
    suspend fun list(documentId: Long) = ds.list(documentId)
}