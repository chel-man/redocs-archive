package com.redocs.archive.framework

import android.util.Log
import com.redocs.archive.data.files.DataSource
import com.redocs.archive.domain.file.FileInfo
import com.redocs.archive.ui.utils.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL

class FilesDataSourceStub(private val filesDir: String) : DataSource {

    private val files = mutableListOf<FileInfo>()
    private var maxId = 0L
    private val filesByDocument = mutableMapOf<Long,MutableList<Long>>()

    init{
        for(f in File(filesDir).listFiles()) {
            if (f.name.startsWith("file_")) {
                f.delete()
            }
        }
        Log.d("#FDS","FILES CLEARED")
    }

    override suspend fun update(file: FileInfo) = withContext(Dispatchers.IO) {
        val ind = files.indexOf(file)
        files.removeAt(ind)
        files.add(ind,file)
    }

    override suspend fun getContent(id: Long): InputStream = withContext(Dispatchers.IO) {
        val index = files.indexOf(FileInfo(id,"",0))
        if(index > -1){
            val f = File(filesDir,"${files[index].intName}")
            if(f.exists())
                return@withContext FileInputStream(f)
        }
        throw NotFoundException(id,"File with ID = $id not found")
    }

    override suspend fun getUrl(id: Long): URL = withContext(Dispatchers.IO) { URL("") }

    override suspend fun list(parentId: Long): List<FileInfo> = withContext(Dispatchers.IO) {
        delay(500)
        val fl = filesByDocument[parentId] ?: listOf<Long>()
        val l = mutableListOf<FileInfo>()
        for(id in fl){
            findById(id)?.run{
                l += this
            }
        }
        l
    }

    private fun findById(id: Long): FileInfo? {
        val i = files.indexOf(FileInfo(id,"",0))
        if(i > -1)
            return files[i]
        return null
    }

    override suspend fun delete(file: FileInfo) = withContext(Dispatchers.IO) {
        for(es in filesByDocument.entries){
            for((i, _) in es.value.withIndex()){
                val fi = files[i]
                if(fi.id == file.id){
                    val f = File(filesDir,"${fi.intName}")
                    if(f.exists())
                        f.delete()
                    es.value.removeAt(i)
                    return@withContext
                }
            }
        }
    }

    override suspend fun upload(
        documentId: Long,
        name: String,
        inputStream: InputStream
    ) = withContext(Dispatchers.IO){

        val buff = ByteArray(1024)
        var size = 0L
        val id = ++maxId
        val iname = "file_$id"
        openFileOutputStream(iname).use {
            while(true){
                val bytes = inputStream.read(buff)
                if(bytes == -1)
                    break
                it.write(buff,0, bytes)
                size += bytes
            }
        }
        files += FileInfo(id,name,size,iname)
        var fs = filesByDocument[documentId]
        if(fs == null) {
            fs = mutableListOf()
            filesByDocument[documentId] = fs
        }
        fs.add(id)
        Unit
    }

    private fun openFileOutputStream(name: String): OutputStream =
        FileOutputStream(File(filesDir,name))

}