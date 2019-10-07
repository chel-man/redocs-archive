package com.redocs.archive.domain.document

import java.util.*

data class Document (
    val id: Long,
    val name: String,
    val fields: Collection<Field> = emptyList(),
    val filesCount: Int,
    val created: Date,
    val updated: Date){

    open class Field(
        val id: Long,
        val title: String,
        val type: FieldType,
        val value: Any?
    )

    class DictionaryField(
        id: Long,
        title: String,
        val dictionaryId: Long,
        value: Any?

    ) : Field(id,title,FieldType.Dictionary,value)

}


enum class DataType {
    Text,
    Integer,
    Decimal,
    Date,
    DictionaryEntry
}

enum class FieldType(val dataType: DataType) {
    Integer(DataType.Integer),
    Decimal(DataType.Decimal),
    Date(DataType.Date),
    Text(DataType.Text),
    LongText(DataType.Text),
    Dictionary(DataType.DictionaryEntry),
    MVDictionary(DataType.DictionaryEntry)
}