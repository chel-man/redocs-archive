package com.redocs.archive.domain.links

data class DocumentLink(
    val id: Long,
    val name: String,
    val sourceId: Long,
    val targetId: Long
)