package com.redocs.archive.framework

import com.redocs.archive.data.dictionary.DictionaryDataSource
import com.redocs.archive.domain.dictionary.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class InMemoryDictionaryDataSource : DictionaryDataSource {
    override suspend fun getEntries(id: Long): List<Dictionary.Entry> =
        withContext(Dispatchers.IO) {
            delay(3000)
            listOf<Dictionary.Entry>(
                Dictionary.Entry(1,"Entry 1"),
                Dictionary.Entry(2,"Entry 2"),
                Dictionary.Entry(3,"Entry 3")
            )
        }
}