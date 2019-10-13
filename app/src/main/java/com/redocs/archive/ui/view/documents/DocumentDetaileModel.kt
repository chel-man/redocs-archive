package com.redocs.archive.ui.view.documents

import com.redocs.archive.domain.document.FieldType
import java.util.*

interface DocumentModelInterface

data class DocumentModel(
    val id: Long,
    val name: String,
    val filesCount: Int = 0,
    val fields: List<FieldModel<*>> = emptyList(),
    val files: Collection<FileModel> = emptyList(),
    val activePanelPos: Int = 0

) : DocumentModelInterface {

    val isStub: Boolean get() = id < 0

    val isDirty: Boolean
        get() {
            for (f in fields) {
                if (f.isDirty)
                    return true
            }
            for (f in files) {
                if (f.isDirty)
                    return true
            }
            return false
        }

    abstract class FieldModel<T>(
        val id: Long,
        val title: String,
        val type: FieldType,
        val value: T?
    ) {

        open val isDirty
            get() =
                value != initValue

        protected var initValue: T? = value

        fun undo() = copy(initValue)

        fun copy(value: T?) = createInstance(value, initValue)

        protected abstract fun createInstance(value: T?, iv: T?): FieldModel<T>

        override fun equals(other: Any?): Boolean {

            if (other === this) return true
            if (javaClass != other?.javaClass) return false

            other as FieldModel<T>

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

    class TextFieldModel(
        id: Long,
        title: String,
        value: String?
    ) : FieldModel<String>(
        id, title, FieldType.Text, value
    ){

        override fun createInstance(value: String?, iv: String?) =
            TextFieldModel(
                id, title, value).apply {
                initValue = iv}

    }

    class IntegerFieldModel(
        id: Long,
        title: String,
        value: Long?
    ) : FieldModel<Long>(
        id, title, FieldType.Integer, value
    ){

        override fun createInstance(value: Long?, iv: Long?) =
            IntegerFieldModel(
                id, title, value).apply {
                initValue = iv}

    }

    class DecimalFieldModel(
        id: Long,
        title: String,
        value: Double?
    ) : FieldModel<Double>(
        id, title, FieldType.Decimal, value
    ){

        override fun createInstance(value: Double?, iv: Double?) =
            DecimalFieldModel(
                id, title, value).apply {
                initValue = iv}

    }

    class DateFieldModel(
        id: Long,
        title: String,
        value: Date?
    ) : FieldModel<Date>(
        id, title, FieldType.Date, value
    ){

        override fun createInstance(value: Date?, iv: Date?) =
            DateFieldModel(
                id, title, value).apply {
                initValue = iv}

    }

    class DictionaryFieldModel(
        id: Long,
        title: String,
        val dictionaryId: Long,
        value: DictionaryEntry?
    ) : FieldModel<DictionaryEntry>(
        id, title, FieldType.Dictionary, value
    ){

        override fun createInstance(value: DictionaryEntry?, iv: DictionaryEntry?) =
            DictionaryFieldModel(
                id, title, dictionaryId,value).apply {
                    initValue = iv}

    }

    class FileModel(
        val id: Long,
        var name: String,
        val size: Long
    ) {
        val isDirty: Boolean
            get() = name != initName

        private val initName = name

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

        fun undo() {
            name = initName
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

