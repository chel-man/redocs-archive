package com.redocs.archive.framework

import com.redocs.archive.data.files.DataSource
import com.redocs.archive.domain.file.FileInfo
import com.squareup.okhttp.OkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

class FilesDataSource (private val serviceUrl: String) : DataSource {

    //private val service:FileService by lazy { RemoteServiceFactory.create<FileService>(serviceUrl)}

    override suspend fun list(parentId: Long): List<FileInfo> {
        OkHttpClient().
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getUrl(id: Long): URL {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getContent(id: Long): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun update(file: FileInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun delete(file: FileInfo) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun upload(documentId: Long, name: String, inputStream: InputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}