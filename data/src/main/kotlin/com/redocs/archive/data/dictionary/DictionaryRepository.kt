package com.redocs.archive.data.dictionary

import com.redocs.archive.domain.dictionary.Dictionary

class DictionaryRepository(private val ds: DictionaryDataSource){

    suspend fun getEntries(id: Long): List<Dictionary.Entry> = ds.getEntries(id)
}