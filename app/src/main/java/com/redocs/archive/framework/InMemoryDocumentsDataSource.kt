package com.redocs.archive.framework

import android.util.Log
import com.redocs.archive.data.documents.DataSource
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import kotlinx.coroutines.delay
import java.util.*

class InMemoryDocumentsDataSource : DataSource {

    private val data = mutableListOf<Document>()

    init {
        for(i in 1..200L)
            data += Document(i,"Document $i",
                listOf(
                    Document.Field(1,"Text",FieldType.Text,"Text value"),
                    Document.Field(2,"Integer long title",FieldType.Integer,12547),
                    Document.Field(3,"long Text",FieldType.LongText,
                        "Text value Text value Text value Text value Text value")
                ),
                Date(), Date())
    }

    override suspend fun get(id: Long): Document = data[0]

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