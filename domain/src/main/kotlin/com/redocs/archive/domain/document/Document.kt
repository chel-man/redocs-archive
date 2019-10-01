package com.redocs.archive.domain.document

import java.util.*

data class Document (
    val id: Long,
    val name: String,
    val fields: Collection<Field> = emptyList(),
    val created: Date,
    val updated: Date){

    data class Field(
        val id: Long,
        val title: String,
        val type: FieldType,
        val value: Any?
    )

}


enum class DataType {
    Text,
    Integer,
    Decimal,
    Date
}

enum class FieldType(val dataType: DataType) {
    Integer(DataType.Integer),
    Decimal(DataType.Decimal),
    Date(DataType.Date),
    Text(DataType.Text),
    LongText(DataType.Text),
    Dictionary(DataType.Text),
    MVDictionary(DataType.Text)
}