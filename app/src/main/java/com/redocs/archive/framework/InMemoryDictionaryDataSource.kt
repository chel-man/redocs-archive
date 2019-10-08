package com.redocs.archive.framework

import com.redocs.archive.data.dictionary.DictionaryDataSource
import com.redocs.archive.domain.dictionary.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class InMemoryDictionaryDataSource : DictionaryDataSource {

    val data = mutableListOf<Dictionary.Entry>()
    init {
        for(i in 1..100L)
            data += Dictionary.Entry(i,"Entry $i")
    }

    override suspend fun getEntries(id: Long): List<Dictionary.Entry> =
        withContext(Dispatchers.IO) {
            //delay(3000)
            data
        }
}