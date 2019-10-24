package com.redocs.archive.framework

import com.redocs.archive.data.files.DataSource
import com.redocs.archive.domain.file.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL

class InMemoryFilesDataSource : DataSource {

    private val files = mutableListOf<FileInfo>(
        FileInfo(1, "File1.txt", 23445),
        FileInfo(2, "File2.txt", 76857),
        FileInfo(3, "File3.txt", 5471354)
    )

    private val filesByDocument = mutableMapOf<Long,MutableList<Int>>(
        1L to mutableListOf(0),
        2L to mutableListOf(1,2)
    )

    override suspend fun update(file: FileInfo) = withContext(Dispatchers.IO) {
        val ind = files.indexOf(file)
        files.removeAt(ind)
        files.add(ind,file)
    }

    override suspend fun getContent(id: Long): InputStream = withContext(Dispatchers.IO) {
        ByteArrayInputStream("Test Content".toByteArray())
    }

    override suspend fun getUrl(id: Long): URL = withContext(Dispatchers.IO) { URL("") }

    override suspend fun list(parentId: Long): List<FileInfo> = withContext(Dispatchers.IO) {
        delay(500)
        val fl = filesByDocument[parentId] ?: listOf<Int>()
        val l = mutableListOf<FileInfo>()
        for(pos in fl){
            l += files[pos]
        }
        l
    }

    override suspend fun delete(file: FileInfo) = withContext(Dispatchers.IO) {
        for(es in filesByDocument.entries){
            for((i,pos) in es.value.withIndex()){
                if(files[pos].id == file.id){
                    es.value.removeAt(i)
                    return@withContext
                }
            }
        }
    }
}