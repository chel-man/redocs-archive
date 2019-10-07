package com.redocs.archive.data.dictionary

import com.redocs.archive.domain.dictionary.Dictionary

interface DictionaryDataSource {
    suspend fun getEntries(id: Long): List<Dictionary.Entry>
}
