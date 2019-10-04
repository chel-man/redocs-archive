package com.redocs.archive.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.Gravity.CENTER
import android.view.Gravity.END
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.LinearLayoutCompat.HORIZONTAL
import androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.R
import com.redocs.archive.data.files.Repository
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import com.redocs.archive.domain.file.File
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.ui.events.DocumentSelectedEvent
import com.redocs.archive.ui.utils.ShortDate
import com.redocs.archive.ui.utils.convertDpToPixel
import com.redocs.archive.ui.view.ActivablePanel
import kotlinx.coroutines.launch
import java.lang.Math.abs
import java.util.*

class DocumentFragment() : Fragment(), EventBusSubscriber, ActivablePanel {

    override var isActive = false

    private lateinit var documentView: DocumentView
    private var firstActivate = true
    private val vm by activityViewModels<DocumentViewModel>()
    private val filesRepo = Repository(ArchiveApplication.filesDataSource)
    private val docsRepo = com.redocs.archive.data.documents.Repository(
                                ArchiveApplication.documentsDataSource)

    init {
        subscribe(DocumentSelectedEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is DocumentSelectedEvent -> vm.documentId = evt.data
        }
    }

    override fun activate() {

        var doc = vm.document.value as DocumentModel?
        if(firstActivate) {
            if (vm.documentId != Long.MIN_VALUE) {
                if(doc?.id != vm.documentId)
                    startLoadDocument()
                else
                    createView(getController(), doc)
                startObservingModel()
            }
        }
        else {
            when {
                vm.documentId == Long.MIN_VALUE ->
                    (view as ViewGroup).removeViewAt(0)

                doc?.id != vm.documentId ->
                    startLoadDocument()
                else ->
                    createView(getController(),doc)
            }
        }
        firstActivate = false
    }

    override fun deactivate() {}

    private fun getController(): Controller {
        val ctr = vm.controller ?: Controller(
            vm,
            docsRepo,
            filesRepo)
        vm.controller = ctr
        return ctr as Controller
    }

    private fun startLoadDocument() {
        vm.document.value = DocumentModel(-vm.documentId, "")
        getController().load()
    }

    private fun startObservingModel(){
        vm.document.observe(this,androidx.lifecycle.Observer {
            Log.d("#DF","model changed")
            it as DocumentModel
            it as DocumentModel
            if(it.isStub)
                createView(getController(),it)
            else
                documentView.update(it)
        })
    }

    private fun createView(controller: Controller,dm: DocumentModel) {

        with(view as ViewGroup) {
            try {
                removeView(documentView)
            }catch (npe: NullPointerException){

            }catch (upa: UninitializedPropertyAccessException){}

            documentView = DocumentView(context,controller,dm)

            addView(documentView)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        ScrollView(context).apply {
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }
        }


    internal open class FieldView(
        context: Context,
        private val position: Int,
        private val type: FieldType,
        title: String,
        value: String?

    ) : TableRow(context) {

        var longClickListener : (Int)->Boolean = { false }

        init {

            addView(
                TextView(context).apply {
                    text = "$title : "
                    gravity = Gravity.END
                })
            addView(
                createValueView(value).apply {
                    layoutParams = generateDefaultLayoutParams().apply {
                        setMargins(0, 0, 10, 0)
                        weight = 1F
                    }
                    setOnLongClickListener {
                        longClickListener(position)
                    }
                })

            setPadding(4)
        }

        protected open fun getAlignment() =
            when(type){
                FieldType.Integer,
                FieldType.Decimal -> Gravity.END
                else ->
                    Gravity.START
            }

        protected open fun createValueView(value: String?) =
            TextView(context).apply {
                text = value ?: ""
                gravity = getAlignment()
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = 5f
                    setStroke(1, Color.BLACK)}
            }

    }

    internal class DateFieldView(
        context: Context?,
        position: Int,
        title: String,
        value: String
    ) : FieldView(
        context as Context,
        position,
        FieldType.Date,
        title,
        value
    )

    private class DocumentView(
        context: Context,
        private val controller: Controller,
        private var dm: DocumentModel

    ) : LinearLayoutCompat(context), ContextActionSource {

        override val lockContent = false

        init{
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }
            orientation = VERTICAL
            if(dm.isStub) {
                addView(
                    ProgressBar(context).apply {
                        this.isIndeterminate = true
                    })
                addView(View(context))
            }
            else {
                addView(
                    FieldListView(context,dm.fields,::onFieldLongClick))
                if(dm.filesCount == 0)
                    addView(View(context))
                else
                    addView(
                        FileListView(context,controller, dm.files, dm.filesCount))
            }
        }

        fun update(model: DocumentModel) {

            if(model.fields != dm.fields) {
                removeViewAt(0)
                addView(
                    FieldListView(context,model.fields,::onFieldLongClick),0)

            }
            if(model.filesCount > 0) {
                if(dm.isStub || dm.files != model.files) {
                    removeViewAt(1)
                    addView(
                        FileListView(context, controller, model.files, model.filesCount), 1
                    )
                }
            }
            dm = model
        }

        private fun onFieldLongClick(position: Int): Boolean {
            //EventBus.publish(ContextActionRequestEvent(this))
            DocumentFieldEditorDialog(
                Editor(context/*dm.fields[position]*/)
            ).show((context as AppCompatActivity).supportFragmentManager,"Editor")
            return true
        }

        override fun createContextActionMenu(inflater: MenuInflater, menu: Menu) {
        }

        override fun onDestroyContextAction() {
        }

        override fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean {
            return true
        }

    }

    private class FileListView(
        context: Context,
        controller: Controller,
        files: Collection<FileModel>,
        filesCount: Int
    ) : CardView(
        context
    ) {

        init{
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(5)
                radius = convertDpToPixel(17,context).toFloat()
            }
            //preventCornerOverlap = true
            cardElevation = convertDpToPixel(10,context).toFloat()

            addView(
                LinearLayoutCompat(context).apply {
                    layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    orientation = VERTICAL

                    val parent = this

                    addView(
                        LinearLayoutCompat(context).apply {
                            layoutParams = LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setBackgroundColor(ContextCompat.getColor(
                                    context, R.color.colorPrimary
                                ))
                            }
                            orientation = HORIZONTAL

                            addView(
                                TextView(context).apply {
                                    text = "Files ($filesCount)"
                                    setTextColor(Color.WHITE)
                                    gravity = Gravity.CENTER_VERTICAL
                                    setPadding(35,0,0,0)
                                    layoutParams = LinearLayoutCompat.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1F
                                    )
                                }
                            )

                            addView(
                                ImageButton(context).apply {

                                    setImageDrawable(
                                        AppCompatResources.getDrawable(
                                            context,
                                            R.drawable.ic_view_white_24dp
                                        )?.apply {
                                            DrawableCompat.setTint(
                                                this,Color.WHITE
                                            )
                                        }
                                    )
                                    /*val p = convertDpToPixel(12, context)
                                    setPadding(p, paddingTop, p, paddingBottom)*/
                                    ViewCompat.setTooltipText(
                                        this,
                                        resources.getString(R.string.action_view)
                                    )
                                    setOnClickListener {
                                        loadList(
                                            context,
                                            parent,
                                            it as ImageButton,
                                            controller,
                                            filesCount != files.size)
                                    }
                                }
                            )
                        }
                    )
                    if(!files.isEmpty()) {
                        val tl = TableLayout(context).apply {
                            setColumnStretchable(1, true)
                            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE or
                                    LinearLayout.SHOW_DIVIDER_BEGINNING or
                                    LinearLayout.SHOW_DIVIDER_END
                            //dividerDrawable =
                        }

                        tl.addView(FileListHeader(context))

                        var colored = false

                        for (r in files) {
                            tl.addView(
                                r.toView(context).apply {
                                    setBackgroundColor(if (colored) Color.LTGRAY else Color.TRANSPARENT)
                                })
                            colored = !colored
                        }
                        addView(tl)
                    }
                }
            )

        }

        private fun loadList(
            context: Context,
            container: ViewGroup,
            button: ImageButton,
            controller: Controller,
            closed: Boolean
        ){
            if(closed){
                container.addView(
                    ProgressBar(context).apply {
                        this.isIndeterminate = true
                    })
                button.isEnabled = false
                controller.showFiles()
            }
            else
                controller.hideFiles()
        }

        private class FileListHeader(
            context: Context
        ) : TableRow(context)
        {
            init {

                setBackgroundColor(Color.LTGRAY)
                addView(
                    TextView(context).apply {
                        text = resources.getString(R.string.file_id_title)
                        gravity = CENTER
                    })
                addView(
                    TextView(context).apply {
                        text = resources.getString(R.string.file_name_title)
                        gravity = CENTER
                    })
                addView(
                    TextView(context).apply {
                        text = resources.getString(R.string.file_size_title)
                        gravity = CENTER
                    })

                //setPadding(4)
            }

        }

        private class FileView(
            context: Context,
            val fileId: Long,
            val name: String,
            val size: Long
        ) : TableRow(context)
        {
            init {
                addView(
                    TextView(context).apply {
                        text = "$fileId"
                    })
                addView(
                    TextView(context).apply {
                        text = "$name"
                    })
                addView(
                    TextView(context).apply {
                        text = "$size"
                        gravity = END
                    })

                //setPadding(14)

            }
        }

        private inline fun FileModel.toView(context: Context) = FileView(context,id,name,size)
    }



    private class FieldListView(
        context: Context,
        fields: Collection<FieldModel>,
        longClickListener: (Int)->Boolean
    ) : CardView(
        context
    ) {

        init {

            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(5)
                radius = convertDpToPixel(17,context).toFloat()
            }
            preventCornerOverlap = true
            cardElevation = convertDpToPixel(10,context).toFloat()

            val grid = TableLayout(context).apply {
                setColumnStretchable(1,true)
                //setColumnShrinkable(1,true)
            }
            for (fv in createViews(context, fields)) {
                fv.longClickListener = longClickListener
                grid.addView(fv)
            }
            addView(grid)
        }

        fun allowClose() = true

        private fun createViews(context: Context, fields: Collection<FieldModel>): Collection<DocumentFragment.FieldView> {
            val l = mutableListOf<DocumentFragment.FieldView>()
            var i = 0
            for(fm in fields)
                l += createFieldView(context,i++,fm)
            return l
        }

    }
}

interface DocumentModelInterface

private data class DocumentModel(
    val id: Long,
    val name: String,
    val filesCount: Int = 0,
    val fields: List<FieldModel> = emptyList(),
    val files: Collection<FileModel> = emptyList()
) : DocumentModelInterface {

    val isStub: Boolean get() = id < 0

    val isDirty: Boolean get() {
            for(f in fields){
                if(f.isDirty)
                    return true
            }
            return false
        }
}

private data class FieldModel(
    val id: Long,
    val title: String,
    val type: FieldType,
    val value: Any?,
    val newValue: Any?
){

    val isDirty get() = value != newValue

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FieldModel

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

private data class FileModel(
    val id: Long,
    val name: String,
    val size: Long
){
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

private inline fun Document.Field.toModel() =
    FieldModel(id,title,type,value,value)

private inline fun Document.toModel() =
    DocumentModel(id,name,filesCount,fields.map { it.toModel() })

private inline fun File.toModel() =
    FileModel(id,name,size)

interface DocumentControllerInterface

private class Controller(
    private val vm: DocumentViewModel,
    private val documentRepository: com.redocs.archive.data.documents.Repository,
    private val filesRepository: Repository

) : DocumentControllerInterface {

    private val scope = vm.coroScope

    fun load() {
        scope.launch {
            val id = abs((vm.document.value as DocumentModel).id)
            vm.document.value = documentRepository.get(id).toModel()
        }
    }

    fun showFiles() {
        scope.launch {
            val d = vm.document.value as DocumentModel
            val files = filesRepository.list(d.id).map{ it.toModel()}
            vm.document.value = d.copy(files = files)
        }
    }

    fun hideFiles() {
        val d = vm.document.value as DocumentModel
        vm.document.value = d.copy(files = emptyList())
    }
}

class DocumentViewModel : ViewModel() {

    var documentId: Long = Long.MIN_VALUE
    val coroScope= viewModelScope
    var document = MutableLiveData<DocumentModelInterface?>()
    var controller: DocumentControllerInterface? = null
    var topField = 0
}

internal class Editor(context: Context) : EditText(context)

class DocumentFieldEditorDialog() : DialogFragment() {

    internal constructor(editor:Editor) : this(){
        this.editor = editor
    }

    private lateinit var editor: Editor

    init {
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context as Context)
            .setView(editor)
            .setNegativeButton("Cancel",{dialog, which ->  })
            .setPositiveButton("Save",{dialog, which ->  })
            .create()
    }
}

private fun createFieldView(context: Context, position: Int,fm: FieldModel): DocumentFragment.FieldView =
    when(fm.type){
        FieldType.Text,
        FieldType.Integer,
        FieldType.Decimal,
        FieldType.LongText,
        FieldType.Dictionary,
        FieldType.MVDictionary -> DocumentFragment.FieldView(context, position,fm.type,fm.title,"${fm.value}")
        FieldType.Date -> DocumentFragment.DateFieldView(
            context,
            position,
            fm.title,
            if(fm.value == null) "" else ShortDate.format(context,fm.value as Date))
        else ->
            throw ClassNotFoundException("Field of type ${fm.type} not found")
    }