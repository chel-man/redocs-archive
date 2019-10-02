package com.redocs.archive.data.files

import com.redocs.archive.domain.file.File

class Repository(private val ds: DataSource){

    suspend fun list(parentId: Long): List<File> = ds.list(parentId)
}