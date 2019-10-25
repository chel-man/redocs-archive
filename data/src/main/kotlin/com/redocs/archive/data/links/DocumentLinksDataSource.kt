package com.redocs.archive.data.links

import com.redocs.archive.domain.links.DocumentLink

interface DocumentLinksDataSource {
    suspend fun list(documentId: Long): List<DocumentLink>
}