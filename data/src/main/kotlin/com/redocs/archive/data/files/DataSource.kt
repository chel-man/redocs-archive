package com.redocs.archive.data.files

import com.redocs.archive.domain.file.FileInfo
import java.io.InputStream
import java.net.URL

interface DataSource {
    suspend fun list(parentId: Long): List<FileInfo>
    suspend fun getUrl(id: Long): URL
    suspend fun getContent(id: Long): InputStream
    suspend fun update(file: FileInfo)
    suspend fun delete(file: FileInfo)
    suspend fun upload(documentId: Long, name: String, inputStream: InputStream)
}