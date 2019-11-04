package com.redocs.archive.data.files

import com.redocs.archive.domain.file.FileInfo
import java.io.InputStream
import java.net.URL

class Repository(private val ds: DataSource){

    suspend fun list(parentId: Long): List<FileInfo> = ds.list(parentId)
    suspend fun getUrl(id: Long): URL = ds.getUrl(id)
    suspend fun getContent(id: Long): InputStream = ds.getContent(id)
    suspend fun update(file: FileInfo) = ds.update(file)
    suspend fun delete(file: FileInfo) = ds.delete(file)
    suspend fun upload(documentId: Long, name: String, inputStream: InputStream) =
        ds.upload(documentId,name,inputStream)
}