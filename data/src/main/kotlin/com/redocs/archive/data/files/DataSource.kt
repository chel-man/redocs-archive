package com.redocs.archive.data.files

import com.redocs.archive.domain.file.File

interface DataSource {
    suspend fun list(parentId: Long): List<File>
}