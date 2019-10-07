package com.redocs.archive.domain.dictionary

class Dictionary(val id: Long) {

    data class Entry(
        val id: Long,
        val text: String
    ){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Entry

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }
}