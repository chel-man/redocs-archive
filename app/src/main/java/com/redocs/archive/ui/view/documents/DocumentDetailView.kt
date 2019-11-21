package com.redocs.archive.ui.view.documents

import android.content.Context
import android.graphics.Color
import android.view.*
import android.view.Gravity.CENTER
import android.view.Gravity.END
import android.widget.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.redocs.archive.*
import com.redocs.archive.domain.document.FieldType
import com.redocs.archive.domain.file.FileInfo
import com.redocs.archive.framework.EventBus
import com.redocs.archive.ui.events.ContextActionRequestEvent
import com.redocs.archive.ui.utils.*
import com.redocs.archive.ui.view.button.ImageButton48
import com.redocs.archive.ui.view.panels.StackPanel
import java.util.*


class DocumentDetailView(
    context: Context,
    private val controller: Controller,
    private var dm: DocumentModel

) : LinearLayoutCompat(context), ContextActionSource {

    override val lockContent = false

    private var actionMode: ActionMode? = null
    private lateinit var panel: StackPanel

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(2)
        }

        orientation = VERTICAL
        if(dm.isStub) {
            addView(
                ProgressBar(context).apply {
                    this.isIndeterminate = true
                })
        }
        else{
            panel = createStackPanel()
            addView(panel)
            addFabs()
        }
    }

    fun update(model: DocumentModel) {

        removeViewAt(0)
        dm = model
        panel = createStackPanel()
        addView(panel)

        checkActionMode()
        addFabs()

    }

    private fun createAddFileButton() =
        FloatingActionButton(context).apply {
            setImageResource(R.drawable.ic_add_white_24_dp)
            setOnClickListener {
                onFileAction(DocumentModel.FileModel.EmptyFileModel,Action.ADD)
            }
        }

    private fun addFabs(){
        if(panel.isEnabled && dm.filesCount == 0)
            panel.addView(createAddFileButton())
    }

    private fun addFile(){
        controller.addFile(context,dm.id,{})
    }

    private fun createStackPanel(): StackPanel =

        StackPanel(context, dm.activePanelPos).apply {

            addPanel("Attributes",FieldListView(
                context,
                dm.fields,
                ::editField,
                ::onClearFieldvalue
            ))

            if(dm.filesCount > 0) {
                val fl = FileListView(context, createAddFileButton()).apply {
                    actionListener = ::onFileAction
                    longClickListener = ::editFile
                }
                fl.files = dm.files
                addPanel(
                    "Files ( ${dm.filesCount} )", fl
                )
            }
            activationListener = { pos ->

                isEnabled = false
                if(pos == 0)
                    controller.showFields()
                else
                    controller.showFiles()
            }
    }

    private fun onFileAction(fm: DocumentModel.FileModel, action: Action) {
        when(action){
            Action.ADD -> addFile()
            Action.VIEW -> controller.viewFile(context,fm)
            Action.DELETE -> controller.deleteFile(fm)
        }
    }

    private fun editFile(fm: DocumentModel.FileModel): Boolean {
        controller.editFile(context,fm)
        return true
    }

    private fun onClearFieldvalue(pos: Int){
        controller.clearFieldValue(pos)
    }

    private fun checkActionMode() {
        if (dm.isDirty) {
            if (actionMode == null)
                EventBus.publish(ContextActionRequestEvent(this))
            panel.isEnabled = false
        } else
            actionMode?.finish()

    }

    private fun editField(position: Int): Boolean {

        val field = dm.fields[position]
        controller.editField(context,field,position)
        return true
    }

    private fun DocumentModel.FileModel.toDomainFileInfo() = FileInfo(id,name,size)

    override fun createContextActionMenu(mode: ActionMode, inflater: MenuInflater, menu: Menu) {
        actionMode = mode
    }

    override fun onDestroyContextAction() {
        actionMode = null
        controller.undo()
    }

    override fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean {
        return true
    }

    private class FileListView(
        context: Context,
        private val addButton: ImageButton
    ) : LinearLayoutCompat(
        context
    ) {

        var longClickListener: (DocumentModel.FileModel) -> Boolean = { false }
        var actionListener: (DocumentModel.FileModel, Action)->Unit = {id, action ->  }

        var files: Collection<DocumentModel.FileModel> = emptyList()
            set(value){
                if(!value.isEmpty()) {
                    val tl = TableLayout(context).apply {
                        setPadding(10,0,10,0)

                        setColumnShrinkable(1, true)
                        setColumnStretchable(1, true)

                        /*showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE or
                                LinearLayout.SHOW_DIVIDER_BEGINNING or
                                LinearLayout.SHOW_DIVIDER_END
                        dividerDrawable = ShapeDrawable(RectShape()).apply {
                            paint.color = Color.BLACK
                            bounds = Rect()
                        }*/

                        addView(
                            FileListHeader(
                                context
                            )
                        )

                        var colored = false

                        for (fm in value) {
                            addView(
                                fm.toView(context).apply {
                                    minimumHeight = dp48pixels()
                                    setBackgroundColor(if (colored) Color.LTGRAY else Color.TRANSPARENT)
                                    actionListener = {id, action ->
                                        this@FileListView.actionListener(fm, action)
                                    }
                                    setOnLongClickListener{
                                        longClickListener(fm)
                                    }
                                })
                            colored = !colored
                        }
                    }

                    removeViewAt(0)
                    addView(ScrollView(context).apply {
                        layoutParams = LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f)
                        addView(tl)
                    })
                    addView(
                        FrameLayout(context).apply {
                            addView(addButton)
                        })
                }
        }

        init {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            orientation = VERTICAL
            addView(ProgressBar(context).apply {
                isIndeterminate = true
            })

            //(addButton.parent as? ViewGroup)?.removeView(addButton)
        }

        private class FileListHeader(
            context: Context
        ) : TableRow(context) {
            init {
                minimumHeight = dp48pixels()
                gravity = Gravity.CENTER_VERTICAL
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
                addView(
                    TextView(context).apply {
                        minimumWidth = dp48pixels()
                    })
                addView(
                    TextView(context).apply {
                        minimumWidth = dp48pixels()
                    })

            }

        }

        private class FileView(
            context: Context,
            val fileId: Long,
            val name: String,
            val size: Long
        ) : TableRow(context) {

            var actionListener: (Long,Action)->Unit = {l, action ->  }

            init {
                gravity = Gravity.CENTER_VERTICAL
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
                        setPadding(0,0,15,0)
                    })
                addView(
                    ImageButton48(context).apply {
                        layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                        )

                        setIcon(
                            R.drawable.ic_view_white_24dp,
                            getColor(R.color.colorPrimary))

                        setOnClickListener {
                            actionListener(fileId, Action.VIEW)
                        }
                    })
                addView(
                    ImageButton48(context).apply {
                        layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                        )

                        setIcon(
                            R.drawable.ic_delete_white_24dp,
                            Color.RED)

                        setOnClickListener {
                            actionListener(fileId, Action.DELETE)
                        }
                    })
            }
        }

        private inline fun DocumentModel.FileModel.toView(context: Context) =
            FileView(context, id, name, size)
    }

    private class FieldListView(
        context: Context,
        fields: Collection<DocumentModel.FieldModel<*>>,
        longClickListener: (Int) -> Boolean,
        clearValueListener: (Int)->Unit
    ) : ScrollView(
        context
    ) {

        init {

            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            /*val card = CardView(context).apply {
                preventCornerOverlap = true
                cardElevation = convertDpToPixel(10, context).toFloat()

                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5)
                    radius = convertDpToPixel(17, context).toFloat()
                }
                preventCornerOverlap = true
                cardElevation = convertDpToPixel(10, context).toFloat()
            }*/

            val card = LinearLayoutCompat(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5)
                }
            }

            val grid = TableLayout(context).apply {
                setColumnShrinkable(2, true)

                addView(
                    TableRow(context).apply {
                        addView(View(context))
                        addView(HorizontalLine(context))
                        addView(HorizontalLine(context))
                        addView(HorizontalLine(context))
                        setPadding(0)
                    }
                )
            }

            for ((i,fm) in fields.withIndex()) {
                with(fm.toView(context,i)) {
                    this.longClickListener = longClickListener
                    this.clearValueListener = clearValueListener
                    val fv = this
                    with(grid) {
                        addView(fv)
                        addView(
                            TableRow(context).apply {
                                addView(View(context))
                                addView(HorizontalLine(context))
                                addView(HorizontalLine(context))
                                addView(HorizontalLine(context))
                                setPadding(0)
                            }
                        )
                    }
                }
            }

            card.addView(grid)
            addView(card)
        }

        fun allowClose() = true

        private fun DocumentModel.FieldModel<*>.toView(
            context: Context,
            position: Int
        ): FieldView<*> =
            when (type) {
                FieldType.LongText,
                FieldType.Text,
                FieldType.Dictionary,
                FieldType.MVDictionary ->
                    TextBasedFieldView(
                        context, position, type, title, isDirty, (value ?: "").toString()
                    )

                FieldType.Integer ->
                    IntegerFieldView(
                        context, position, type, title, isDirty, value as Long?
                    )

                FieldType.Decimal ->
                    DecimalFieldView(
                        context, position, type, title, isDirty, value as Double?
                    )

                FieldType.Date -> DateFieldView(
                    context,
                    position,
                    title,
                    isDirty,
                    value as Date?
                )
                else ->
                    throw ClassNotFoundException("Field of type ${type} not found")
            }

        private abstract class FieldView<T>(
            context: Context,
            private val position: Int,
            private val type: FieldType,
            title: String,
            isDirty: Boolean,
            value: T?
        ) : TableRow(context) {

            var longClickListener: (Int) -> Boolean = { false }
            var clearValueListener: (Int) -> Unit = {  }

            init {
                minimumHeight = dp48pixels()
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                        text = "$title : "
                        gravity = Gravity.END
                    })

                addView(
                    View(context).apply {
                        layoutParams = LayoutParams(
                            1,
                            LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(getColor(R.color.colorPrimaryDark))
                    })

                addView(
                    createValueView(format(value), isDirty).apply {
                        setOnLongClickListener {
                            longClickListener(position)
                        }
                        setPadding(10)
                    }
                )

                addView(
                    ImageButton48(context).apply {
                        setIcon(
                            R.drawable.ic_clear_white_24dp,
                            Color.RED)
                        layoutParams = LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            layoutParams.height
                        )
                        setOnClickListener {
                            clearValueListener(position)
                        }
                    }
                )
                setPadding(4)
            }

            protected open fun format(v: T?): String = (v ?: "").toString()

            protected open fun getAlignment() = Gravity.START

            protected open fun createValueView(value: String, isDirty: Boolean) =
                TextView(context).apply {
                    text = value
                    gravity = getAlignment()
                    if (isDirty) {
                        //underLine()
                        textBold()
                        setTextColor(Color.BLACK)
                        setBackgroundColor(
                            getColor(
                                R.color.colorPrimaryLight))
                    }
                }

        }

        private class DateFieldView(
            context: Context?,
            position: Int,
            title: String,
            dirty: Boolean,
            value: Date?
        ) : FieldView<Date>(
            context as Context,
            position,
            FieldType.Date,
            title,
            dirty,
            value
        ) {
            override fun format(v: Date?): String =
                if (v == null) "" else ShortDate.format(context, v)
        }

        private class TextBasedFieldView(
            context: Context?,
            position: Int,
            type: FieldType,
            title: String,
            dirty: Boolean,
            value: String?
        ) : FieldView<String>(
            context as Context,
            position,
            type,
            title,
            dirty,
            value
        )

        private class IntegerFieldView(
            context: Context?,
            position: Int,
            type: FieldType,
            title: String,
            dirty: Boolean,
            value: Long?
        ) : FieldView<Long>(
            context as Context,
            position,
            type,
            title,
            dirty,
            value
        ) {
            override fun getAlignment() = Gravity.END
        }

        private class DecimalFieldView(
            context: Context?,
            position: Int,
            type: FieldType,
            title: String,
            dirty: Boolean,
            value: Double?
        ) : FieldView<Double>(
            context as Context,
            position,
            type,
            title,
            dirty,
            value
        ) {
            override fun getAlignment() = Gravity.END
        }

        /*private class DictionaryFieldView(
            context: Context?,
            position: Int,
            title: String,
            dirty: Boolean,
            value: DocumentModel.DictionaryEntry?
        ) : FieldView<DocumentModel.DictionaryEntry>(
            context as Context,
            position,
            FieldType.Dictionary,
            title,
            dirty,
            value
        ) {
            override fun format(v: Date?): String =
                if (v == null) "" else ShortDate.format(context, v)
        }*/

        private class HorizontalLine(context: Context) : View(context){

            init {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    1
                )
                setBackgroundColor(getColor(R.color.colorPrimaryDark))

            }
        }
    }
}