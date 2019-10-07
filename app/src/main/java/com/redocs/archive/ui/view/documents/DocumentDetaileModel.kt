package com.redocs.archive.ui.view.documents

import com.redocs.archive.asLongOrOriginal
import com.redocs.archive.domain.document.FieldType

interface DocumentModelInterface

data class DocumentModel(
    val id: Long,
    val name: String,
    val filesCount: Int = 0,
    val fields: List<FieldModel> = emptyList(),
    val files: Collection<FileModel> = emptyList()
) : DocumentModelInterface {

    val isStub: Boolean get() = id < 0

    val isDirty: Boolean
        get() {
            for (f in fields) {
                if (f.isDirty)
                    return true
            }
            return false
        }

    open class FieldModel(
        val id: Long,
        val title: String,
        val type: FieldType,
        val value: Any?
    ) {

        open val isDirty
            get() =
                value?.asLongOrOriginal() != initValue?.asLongOrOriginal()

        protected var initValue: Any? = value

        fun undo() = copy(initValue)

        fun copy(value: Any?) = createInstance(value, initValue)

        protected open fun createInstance(value: Any?, iv: Any?) =
            FieldModel(id, title, type, value).apply { initValue = iv }

        override fun equals(other: Any?): Boolean {

            if (other === this) return true
            if (javaClass != other?.javaClass) return false

            other as FieldModel

            if (id != other.id) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String = "id: $id iv: $initValue v: $value"
    }

    class DictionaryFieldModel(
        id: Long,
        title: String,
        type: FieldType,
        val dictionaryId: Long,
        value: DictionaryEntry?
    ) : FieldModel(
        id, title, type, value
    ){

        override fun createInstance(value: Any?, iv: Any?) =
            DictionaryFieldModel(
                id, title, type, dictionaryId,value as DictionaryEntry?).apply {
                    initValue = iv}

    }

    data class FileModel(
        val id: Long,
        val name: String,
        val size: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FileModel

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    data class DictionaryEntry(
        val id: Long,
        val text: String
    ){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DictionaryEntry

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString(): String {
            return text
        }
    }
}

