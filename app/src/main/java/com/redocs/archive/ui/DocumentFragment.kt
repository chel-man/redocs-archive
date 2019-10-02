package com.redocs.archive.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Gravity.CENTER
import android.view.Gravity.END
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.LinearLayoutCompat.HORIZONTAL
import androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private lateinit var scope: CoroutineScope

class DocumentFragment() : Fragment(), EventBusSubscriber, ActivablePanel {

    override var isActive = false

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        scope = vm.coroScope
    }

    override fun activate() {
        var doc = vm.document
        if(doc?.id != vm.documentId) {
            if (vm.documentId == Long.MIN_VALUE)
                return
            doc = DocumentStub(-vm.documentId)
            createView(doc)
        }
    }

    override fun deactivate() {}

    private fun DocumentStub(id: Long) =
        Document(id,"",filesCount = 0, created = Date(), updated = Date())

    private inline fun Document.isStub() = id<0

    private fun createView(document: Document) {

        with(view as ViewGroup) {
            try {
                removeViewAt(0)
            }catch (npe: NullPointerException){}
            val docView = DocumentView(context, filesRepo, document)
            addView(docView)
            if(document.isStub()){
                scope.launch {
                    val  doc = docsRepo.get(-document.id)
                    vm.document = doc
                    withContext(Dispatchers.Main){
                        docView.updateFields(doc.fields)
                        docView.updateFiles(doc.filesCount)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        ScrollView(context)
        /*LinearLayoutCompat(context)*/.apply {
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }

        }


    internal open class FieldView(
        context: Context,
        private val type: FieldType,
        title: String,
        value: String?

    ) : TableRow(context)
    {

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
        df: Document.Field,
        value: String
    ) : FieldView(
        context as Context,
        FieldType.Date,
        df.title,
        value
    )

    private class DocumentView(
        context: Context,
        repo: Repository,
        doc: Document?
    ) : LinearLayoutCompat(context)
    {
        init{
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }
            orientation = VERTICAL

            if(doc != null) {
                addView(
                    FieldListView(context, doc.fields))
                addView(
                    FileListView(
                        context, doc.id, repo, doc.filesCount))
            }
        }
    }

    private class FileListView(
        context: Context,
        documentId: Long,
        repo: Repository,
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
                                        loadList(context, parent, it as ImageButton,repo, documentId)
                                    }
                                }
                            )
                        }
                    )
                }
            )
        }

        private var closed = true

        private fun loadList(context: Context, vg: ViewGroup, button: ImageButton, repo: Repository, id: Long){
            if(closed){
                createFileListTable(context,vg,button,repo, id)
            }
            else{
                vg.removeViewAt(1)
            }

            closed = !closed
        }

        private fun createFileListTable(
            context: Context,
            container: ViewGroup,
            button: ImageButton,
            repo: Repository,
            id: Long
        ){

            val pb = ProgressBar(context).apply {
                this.isIndeterminate = true
            }
            container.addView(pb)
            button.isEnabled = false
            scope.launch {
                val files = withContext(Dispatchers.IO){
                    repo.list(id).map { it.toView(context) }
                }

                withContext(Dispatchers.Main){
                    val tl = TableLayout(context).apply {
                        setColumnStretchable(1,true)
                        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE or
                                LinearLayout.SHOW_DIVIDER_BEGINNING or
                                LinearLayout.SHOW_DIVIDER_END
                        //dividerDrawable =
                    }
                    tl.addView(FileListHeader(context))
                    var colored = false
                    for(r in files) {
                        tl.addView(
                            r.apply {
                                setBackgroundColor(if(colored) Color.LTGRAY else Color.TRANSPARENT)
                            })
                        colored = !colored
                    }
                    with(container){
                        removeView(pb)
                        addView(tl)
                    }
                    button.isEnabled = true
                }
            }
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

        private inline fun File.toView(context: Context) = FileView(context,id,name,size)
    }



    private class FieldListView(
        context: Context,
        fields: Collection<Document.Field>
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
            for (fv in createViews(context, fields))
                grid.addView(fv)
            addView(grid)
        }

        fun allowClose() = true

        private fun createViews(context: Context, fields: Collection<Document.Field>): Collection<DocumentFragment.FieldView> {
            val l = mutableListOf<DocumentFragment.FieldView>()
            for(df in fields)
                l += createFieldView(context,df)
            return l
        }

    }
}

class DocumentViewModel : ViewModel() {

    var documentId: Long = Long.MIN_VALUE
    val coroScope= viewModelScope
    var document: Document? = null
    var topField = 0
}

private fun createFieldView(context: Context,df: Document.Field): DocumentFragment.FieldView =
    when(df.type){
        FieldType.Text,
        FieldType.Integer,
        FieldType.Decimal,
        FieldType.LongText,
        FieldType.Dictionary,
        FieldType.MVDictionary -> DocumentFragment.FieldView(context, df.type,df.title,"${df.value}")
        FieldType.Date -> DocumentFragment.DateFieldView(
            context,
            df,
            if(df.value == null) "" else ShortDate.format(context,df.value as Date))
        else ->
            throw ClassNotFoundException("Field of type ${df.type} not found")
    }
