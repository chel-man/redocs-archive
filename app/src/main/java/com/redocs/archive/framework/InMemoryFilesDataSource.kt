package com.redocs.archive.framework

import com.redocs.archive.data.files.DataSource
import com.redocs.archive.domain.file.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class InMemoryFilesDataSource : DataSource {

    override suspend fun list(parentId: Long): List<File> = withContext(Dispatchers.IO) {
        delay(2000)
        listOf(
            File(1, "File 1", 23445),
            File(2, "File 2", 76857)
        )
    }
}