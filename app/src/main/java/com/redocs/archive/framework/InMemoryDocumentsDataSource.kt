package com.redocs.archive.framework

import android.util.Log
import com.redocs.archive.data.documents.DataSource
import com.redocs.archive.domain.dictionary.Dictionary
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import kotlinx.coroutines.delay
import java.util.Date

class InMemoryDocumentsDataSource : DataSource {

    private val data = mutableListOf<Document>()

    init {
        for(i in 1..200L)
            data += Document(i,"Document $i",
                listOf(
                    Document.Field(1,"Text",FieldType.Text,
                        "$i : Text value Text value Text value Text value Text value Text value"),
                    Document.Field(3,"long Text",FieldType.LongText,
                        "Text value Text value Text value Text value Text value Text value Text value Text value Text value"),
                    Document.Field(2,"Integer long title",FieldType.Integer,12547),
                    Document.Field(4,"Decimal",FieldType.Decimal,365.457),
                    Document.Field(5,"Date",FieldType.Date, Date()),
                    Document.DictionaryField(6,"Dictionary",1, Dictionary.Entry(1,""))
                ),
                (i-1).toInt(),Date(), Date())
    }

    override suspend fun get(id: Long): Document {
        delay(500)
        return data[id.toInt()-1]
    }

    override suspend fun list(parentId: Long, start: Int, size: Int): Collection<Document> {
        var end = start + size
        if (start > data.size - 1)
            return listOf()
        if (end > data.size - 1)
            end = data.size
        Log.d("#DocumentRepo", "RESP: $start : $end")
        delay(500)
        return data.subList(start, end)

    }

}