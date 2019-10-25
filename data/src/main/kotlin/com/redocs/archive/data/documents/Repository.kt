package com.redocs.archive.data.documents

import com.redocs.archive.domain.document.Document

class Repository(private val ds: DataSource){

    suspend fun list(parentId: Long, start: Int, size: Int): Collection<Document> =
        ds.list(parentId, start, size)

    suspend fun get(id: Long): Document = ds.get(id)
    suspend fun get(ids: List<Long>): Collection<Document> = ds.get(ids)
}