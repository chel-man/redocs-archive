package com.redocs.archive.ui.view.documents

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import com.redocs.archive.BuildConfig
import com.redocs.archive.asDoubleOrNull
import com.redocs.archive.asLongOrNull
import com.redocs.archive.data.dictionary.DictionaryRepository
import com.redocs.archive.data.files.Repository
import com.redocs.archive.domain.dictionary.Dictionary
import com.redocs.archive.domain.document.DataType
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import com.redocs.archive.domain.file.FileInfo
import com.redocs.archive.ui.utils.*
import com.redocs.archive.ui.view.list.SimpleList
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
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

    fun viewFile(context: Context, fm: DocumentModel.FileModel) {
        scope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir,"temp")
            dir.mkdirs()
            val fis = filesRepository.getContent(fm.id)
            var f = File(dir, fm.name)
            if(f.exists())
                f.delete()
            fis.use {
                val fis = it
                FileOutputStream(f).use {
                    val ba = ByteArray(1204)
                    var bytes = fis.read(ba)
                    while (bytes > 0) {
                        it.write(ba, 0, bytes)
                        bytes = fis.read(ba)
                    }
                }
            }
            f.deleteOnExit()
            withContext(Dispatchers.Main) {
                openFile(context, f)
            }
        }
    }

    fun deleteFile(fm: DocumentModel.FileModel){
        scope.launch {
            filesRepository.delete(fm.toFileInfo())
            refreshFileList()
        }
    }

    fun editField(context: Context, field: DocumentModel.FieldModel<*>, position: Int) {
        val ed = createFieldEditor(context,field)
        ModalDialog(
            ModalDialog.SaveDialogConfig(
                ed,
                title = field.title,
                actionListener = { which ->
                    when (which) {
                        ModalDialog.DialogButton.POSITIVE -> {
                            setFieldValue(position, (ed as CustomEditor<*>).value)
                        }

                    }
                }
            )
        )
            .show((context as AppCompatActivity).supportFragmentManager, "CustomEditor")

    }

    fun editFile(context: Context, file: DocumentModel.FileModel): Boolean {

        val ed = TextCustomEditor(context,file.name).apply { minLength = 1 }
        ModalDialog(
            ModalDialog.SaveDialogConfig(
                ed,
                //title = field.title,
                actionListener = { which ->
                    when (which) {
                        ModalDialog.DialogButton.POSITIVE -> {
                            val dm = documentLive.value as DocumentModel
                            val v = ed.value
                            if(v != null)
                                saveFileInfo(
                                    dm.files.indexOf(file),
                                    FileInfo(
                                        file.id,
                                        v,
                                        file.size)
                                )
                        }
                    }
                }
            )
        )
            .show((context as AppCompatActivity).supportFragmentManager, "CustomEditor")
        return true
    }

    private fun saveFileInfo(position: Int, file: FileInfo) {
        scope.launch {
            filesRepository.update(file)
            refreshFileList()
        }
    }

    private suspend fun refreshFileList(){
        val d = documentLive.value as DocumentModel
        val files = filesRepository.list(d.id).map{ it.toModel()}
        documentLive.value = d.copy(
            files = files,
            activePanelPos = if(files.isEmpty()) 0 else 1,
            filesCount = files.size)
    }

    private fun loadDictionaryEntries(editor: DictionaryEditor, id: Long) = scope.launch {
        val l = loadDictionaryEntries(id)
        withContext(Dispatchers.Main){
            editor.model = SimpleList.ListModel(l)
        }
    }

    private fun openFile(context: Context, f: File) =

        Intent(Intent.ACTION_VIEW).apply {

            // set flag to give temporary permission to external app to use your FileProvider
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            // generate URI, I defined authority as the application ID in the Manifest, the last param is file I want to open
            data = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, f)
            // validate that the device can open your File!
            if (resolveActivity(context.packageManager) != null)
                context.startActivity(this)
            else
                showError(context,"No app to open file ${f.name}")
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


    private fun createFieldEditor(
        context: Context,
        field: DocumentModel.FieldModel<*>
    ): View {

        val value = field.value
        return when (field.type.dataType) {
            DataType.Integer -> IntegerCustomEditor(context, value?.asLongOrNull())
            DataType.Decimal -> DecimalCustomEditor(context, value as Double?)
            DataType.Date -> DateEditor(context, value as Date?)
            DataType.DictionaryEntry -> {
                val editor = DictionaryEditor(
                    context,
                    value as DocumentModel.DictionaryEntry?)

                scope.launch {
                    val l = loadDictionaryEntries(
                        (field as DocumentModel.DictionaryFieldModel).dictionaryId
                    )
                    withContext(Dispatchers.Main){
                        editor.model = SimpleList.ListModel(l)
                    }
                }
                editor
            }
            else ->
                TextCustomEditor(context, value.toString())
        }
    }
}

private fun DocumentModel.FileModel.toFileInfo() =
    FileInfo(id,name,size)