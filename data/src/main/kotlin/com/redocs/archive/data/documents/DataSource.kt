package com.redocs.archive.data.documents

import com.redocs.archive.domain.document.Document

interface DataSource {
    suspend fun list(parentId: Long, start: Int, size: Int): Collection<Document>
    suspend fun get(id: Long): Document
}