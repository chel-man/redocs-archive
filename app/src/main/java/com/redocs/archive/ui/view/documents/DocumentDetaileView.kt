package com.redocs.archive.ui.view.documents

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.Gravity.CENTER
import android.view.Gravity.END
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.redocs.archive.R
import com.redocs.archive.asLongOrNull
import com.redocs.archive.domain.document.DataType
import com.redocs.archive.domain.document.FieldType
import com.redocs.archive.framework.EventBus
import com.redocs.archive.ui.events.ContextActionRequestEvent
import com.redocs.archive.ui.utils.*
import java.util.*


class DocumentDetaileView(
    context: Context,
    private val controller: Controller,
    private var dm: DocumentModel

) : LinearLayoutCompat(context), ContextActionSource {

    override val lockContent = false

    private var actionMode: ActionMode? = null

    init {
        layoutParams = LinearLayoutCompat.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(15)
        }
        orientation = VERTICAL
        if (dm.isStub) {
            addView(
                ProgressBar(context).apply {
                    this.isIndeterminate = true
                })
            addView(View(context))
        } else {
            addView(
                FieldListView(
                    context,
                    dm.fields,
                    ::onFieldLongClick
                )
            )
            if (dm.filesCount == 0)
                addView(View(context))
            else
                addView(
                    FileListView(
                        context,
                        controller,
                        dm.files,
                        dm.filesCount
                    )
                )
        }
    }

    fun update(model: DocumentModel) {

        if (model.fields != dm.fields) {
            removeViewAt(0)
            addView(
                FieldListView(
                    context,
                    model.fields,
                    ::onFieldLongClick
                ), 0
            )

        }
        if (model.filesCount > 0) {
            if (dm.isStub || dm.files != model.files) {
                removeViewAt(1)
                addView(
                    FileListView(
                        context,
                        controller,
                        model.files,
                        model.filesCount
                    ), 1
                )
            }
        }
        dm = model
        checkActionMode()

    }

    private fun checkActionMode() {
        if (dm.isDirty) {
            if (actionMode == null)
                EventBus.publish(ContextActionRequestEvent(this))
        } else
            actionMode?.finish()

    }

    private fun onFieldLongClick(position: Int): Boolean {

        val field = dm.fields[position]
        val ed = createFieldEditor(
            context,
            field.type.dataType,
            field.value
        )
        ModalDialog(
            ModalDialog.SaveDialogConfig(
                ed,
                title = "Редактирование",
                actionListener = { which ->
                    when (which) {
                        ModalDialog.DialogButton.POSITIVE -> {
                            controller.setFieldValue(position, (ed as CustomEditor<*>).value)
                        }

                    }
                }
            )
        )
            .show((context as AppCompatActivity).supportFragmentManager, "CustomEditor")
        return true
    }

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
        controller: Controller,
        files: Collection<DocumentModel.FileModel>,
        filesCount: Int
    ) : CardView(
        context
    ) {

        init {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(5)
                radius = convertDpToPixel(17, context).toFloat()
            }
            //preventCornerOverlap = true
            cardElevation = convertDpToPixel(10, context).toFloat()

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
                                setBackgroundColor(
                                    ContextCompat.getColor(
                                        context, R.color.colorPrimary
                                    )
                                )
                            }
                            orientation = HORIZONTAL

                            addView(
                                TextView(context).apply {
                                    text = "Files ($filesCount)"
                                    setTextColor(Color.WHITE)
                                    gravity = Gravity.CENTER_VERTICAL
                                    setPadding(35, 0, 0, 0)
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
                                                this, Color.WHITE
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
                                            filesCount != files.size
                                        )
                                    }
                                }
                            )
                        }
                    )
                    if (!files.isEmpty()) {
                        val tl = TableLayout(context).apply {
                            setColumnStretchable(1, true)
                            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE or
                                    LinearLayout.SHOW_DIVIDER_BEGINNING or
                                    LinearLayout.SHOW_DIVIDER_END
                            //dividerDrawable =
                        }

                        tl.addView(
                            FileListHeader(
                                context
                            )
                        )

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
        ) {
            if (closed) {
                container.addView(
                    ProgressBar(context).apply {
                        this.isIndeterminate = true
                    })
                button.isEnabled = false
                controller.showFiles()
            } else
                controller.hideFiles()
        }

        private class FileListHeader(
            context: Context
        ) : TableRow(context) {
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
        ) : TableRow(context) {
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

        private inline fun DocumentModel.FileModel.toView(context: Context) =
            FileView(context, id, name, size)
    }


    private class FieldListView(
        context: Context,
        fields: Collection<DocumentModel.FieldModel>,
        longClickListener: (Int) -> Boolean
    ) : CardView(
        context
    ) {

        init {

            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(5)
                radius = convertDpToPixel(17, context).toFloat()
            }
            preventCornerOverlap = true
            cardElevation = convertDpToPixel(10, context).toFloat()

            val grid = TableLayout(context).apply {
                setColumnStretchable(1, true)
                //setColumnShrinkable(1,true)
            }
            for (fv in createViews(context, fields)) {
                fv.longClickListener = longClickListener
                grid.addView(fv)
            }
            addView(grid)
        }

        fun allowClose() = true

        private fun createViews(
            context: Context,
            fields: Collection<DocumentModel.FieldModel>
        ): Collection<FieldView<*>> {
            val l = mutableListOf<FieldView<*>>()
            var i = 0
            for (fm in fields)
                l += createFieldView(context, i++, fm)
            return l
        }

        private fun createFieldView(
            context: Context,
            position: Int,
            fm: DocumentModel.FieldModel
        ): FieldView<*> =
            when (fm.type) {
                FieldType.LongText,
                FieldType.Text,
                FieldType.Dictionary,
                FieldType.MVDictionary ->
                    TextBasedFieldView(
                        context, position, fm.type, fm.title, fm.isDirty, fm.value.toString()
                    )

                FieldType.Integer ->
                    IntegerFieldView(
                        context, position, fm.type, fm.title, fm.isDirty, fm.value?.asLongOrNull()
                    )

                FieldType.Decimal ->
                    DecimalFieldView(
                        context, position, fm.type, fm.title, fm.isDirty, fm.value as Double?
                    )

                FieldType.Date -> DateFieldView(
                    context,
                    position,
                    fm.title,
                    fm.isDirty,
                    fm.value as Date?
                )
                else ->
                    throw ClassNotFoundException("Field of type ${fm.type} not found")
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

            init {

                addView(
                    TextView(context).apply {
                        text = "$title : "
                        gravity = Gravity.END
                    })
                addView(
                    createValueView(format(value), isDirty).apply {
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

            protected open fun format(v: T?): String = (v ?: "").toString()

            protected open fun getAlignment() = Gravity.START

            protected open fun createValueView(value: String, isDirty: Boolean) =
                TextView(context).apply {
                    text = value
                    gravity = getAlignment()
                    background = GradientDrawable().apply {
                        setColor(if (isDirty) Color.YELLOW else Color.TRANSPARENT)
                        cornerRadius = 5f
                        setStroke(1, Color.BLACK)
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

    }

    private fun createFieldEditor(context: Context, dataType: DataType, value: Any?): View =
        when (dataType) {
            DataType.Integer -> IntegerCustomEditor(context, value?.asLongOrNull())
            DataType.Decimal -> DecimalCustomEditor(context, value as Double?)
            else ->
                TextCustomEditor(context, value.toString())
        }

}
