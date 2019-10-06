package com.redocs.archive.ui.view.documents

import androidx.lifecycle.MutableLiveData
import com.redocs.archive.data.files.Repository
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.file.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface DocumentControllerInterface

class Controller(
    private val scope: CoroutineScope,
    private val documentLive: MutableLiveData<DocumentModel>,
    private val documentRepository: com.redocs.archive.data.documents.Repository,
    private val filesRepository: Repository

) : DocumentControllerInterface {

    fun load() {
        scope.launch {
            val id = Math.abs((documentLive.value as DocumentModel).id)
            documentLive.value = documentRepository.get(id).toModel()
        }
    }

    fun showFiles() {
        scope.launch {
            val d = documentLive.value as DocumentModel
            val files = filesRepository.list(d.id).map{ it.toModel()}
            documentLive.value = d.copy(files = files)
        }
    }

    fun hideFiles() {
        val d = documentLive.value as DocumentModel
        documentLive.value = d.copy(files = emptyList())
    }

    fun setFieldValue(position: Int, v: Any?) {
        val d = documentLive.value as DocumentModel
        val l = mutableListOf<DocumentModel.FieldModel>().apply {
            addAll(d.fields)
        }
        l[position]= d.fields[position].copy(v)
        documentLive.value = d.copy(fields = l.toList())
    }

    fun undo() {
        val d = documentLive.value as DocumentModel
        val l = mutableListOf<DocumentModel.FieldModel>()
        for(f in d.fields)
            l += f.undo()
        documentLive.value = d.copy(fields = l.toList())
    }
}

private inline fun Document.Field.toModel() =
    DocumentModel.FieldModel(id, title, type, value)

private inline fun Document.toModel() =
    DocumentModel(
        id,
        name,
        filesCount,
        fields.map { it.toModel() })

private inline fun File.toModel() =
    DocumentModel.FileModel(id, name, size)

