package com.redocs.archive.ui.view.documents

import androidx.lifecycle.MutableLiveData
import com.redocs.archive.asDoubleOrNull
import com.redocs.archive.asLongOrNull
import com.redocs.archive.data.dictionary.DictionaryRepository
import com.redocs.archive.data.files.Repository
import com.redocs.archive.domain.dictionary.Dictionary
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import com.redocs.archive.domain.file.FileInfo
import com.redocs.archive.ui.utils.NotFoundException
import com.redocs.archive.ui.view.list.SimpleList
import kotlinx.coroutines.*
import java.io.File
import java.util.*

interface DocumentControllerInterface

class Controller(
    private val scope: CoroutineScope,
    private val documentLive: MutableLiveData<DocumentModel>,
    private val documentRepository: com.redocs.archive.data.documents.Repository,
    private val filesRepository: Repository,
    private val dictionaryRepository: DictionaryRepository

) : DocumentControllerInterface {

    fun load() {
        scope.launch {
            val id = Math.abs((documentLive.value as DocumentModel).id)
            documentLive.value = documentRepository.get(id).toModel()
        }
    }

    fun showFiles() {

        val d = documentLive.value as DocumentModel
        if(d.files.isEmpty())
            scope.launch {
                val files = filesRepository.list(d.id).map{ it.toModel()}
                documentLive.value = d.copy(files = files, activePanelPos = 1)
            }
        else
            documentLive.value = d.copy(activePanelPos = 1)

    }

    fun showFields() {
        val d = documentLive.value as DocumentModel
        documentLive.value = d.copy(activePanelPos = 0)
    }

    fun <T> setFieldValue(position: Int, v: T?) {
        val d = documentLive.value as DocumentModel
        val l = mutableListOf<DocumentModel.FieldModel<*>>().apply {
            addAll(d.fields)
        }
        l[position] = (d.fields[position] as DocumentModel.FieldModel<T>).copy(v)
        documentLive.value = d.copy(fields = l.toList())
    }

    fun undo() {
        val d = documentLive.value as DocumentModel
        val fls = mutableListOf<DocumentModel.FieldModel<*>>()
        for(f in d.fields)
            fls += f.undo()
        /*for(fl in d.files)
            fl.undo()*/
        documentLive.value = d.copy(fields = fls.toList(), files = d.files)
    }

    fun clearFieldValue(position: Int) {
        setFieldValue(position, null)
    }

    fun viewFile(id: Long) {
        scope.launch {
            val file = filesRepository.getContent(id)
        }
    }

    fun saveFileInfo(position: Int, file: FileInfo) {
        scope.launch {
            filesRepository.update(file)
        }
    }

    fun loadDictionaryEntries(editor: DictionaryEditor, id: Long){
        scope.launch {
            val l = loadDictionaryEntries(id)
            withContext(Dispatchers.Main){
                editor.model = SimpleList.ListModel(l)
            }
        }
    }

    private fun loadDictionaryEntriesSync(id: Long) = runBlocking {
        dictionaryRepository.getEntries(id)
    }

    private suspend fun loadDictionaryEntries(id: Long) =
        dictionaryRepository.getEntries(id).map { it.toModel() }

    private inline fun Document.toModel() =
        DocumentModel(
            id,
            name,
            filesCount,
            fields.map { it.toModel() }
        )

    private inline fun FileInfo.toModel() =
        DocumentModel.FileModel(id, name, size)

    private fun Document.Field.toModel(): DocumentModel.FieldModel<*> {
        return when(type){
            FieldType.Dictionary -> {
                this as Document.DictionaryField
                DocumentModel.DictionaryFieldModel(
                    id, title,
                    dictionaryId,(value as? Dictionary.Entry)?.toModel()
                )
            }
            FieldType.Text -> DocumentModel.TextFieldModel(id,title,value as String?)
            FieldType.Integer -> DocumentModel.IntegerFieldModel(id,title,value?.asLongOrNull())
            FieldType.Decimal -> DocumentModel.DecimalFieldModel(id,title,value?.asDoubleOrNull())
            FieldType.Date -> DocumentModel.DateFieldModel(id,title,value as Date?)
            else ->
                DocumentModel.TextFieldModel(id, title, value as? String)
        }
    }

    private fun Dictionary.Entry.toModel() =
        DocumentModel.DictionaryEntry(id,text)

    private fun loadDictionaryEntryValue(id: Long, de: Dictionary.Entry?): DocumentModel.DictionaryEntry? {
        if(de == null) return null
        val l = loadDictionaryEntriesSync(id)
        val pos = l.indexOf(de)
        if(pos > -1)
            return l[pos].toModel()
        throw NotFoundException(de.id,"dictionary entry ${de.id} not found")
    }

}