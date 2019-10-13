package com.redocs.archive.ui.view.documents

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.*
import android.view.Gravity.CENTER
import android.view.Gravity.END
import android.widget.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.redocs.archive.R
import com.redocs.archive.domain.document.FieldType
import com.redocs.archive.dp48pixels
import com.redocs.archive.framework.EventBus
import com.redocs.archive.getColor
import com.redocs.archive.ui.events.ContextActionRequestEvent
import com.redocs.archive.ui.utils.*
import com.redocs.archive.ui.view.panels.StackPanel
import java.util.*


class DocumentDetaileView(
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
        if (dm.isStub) {
            addView(
                ProgressBar(context).apply {
                    this.isIndeterminate = true

                })
        } else {
            panel = createStackPanel()
            addView(panel)
        }
    }

    fun update(model: DocumentModel) {

        removeViewAt(0)
        dm = model
        panel = createStackPanel()
        addView(panel)
        checkActionMode()

    }

    private fun createStackPanel(): StackPanel {

        return StackPanel(context, dm.activePanelPos).apply {

            addPanel("Поля",FieldListView(
                context,
                dm.fields,
                ::onFieldLongClick
            ))

            if(dm.filesCount > 0) {
                val fl = FileListView(context)
                fl.fiels = dm.files
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

    }

    private fun checkActionMode() {
        if (dm.isDirty) {
            if (actionMode == null)
                EventBus.publish(ContextActionRequestEvent(this))
            panel.isEnabled = false
        } else
            actionMode?.finish()

    }

    private fun onFieldLongClick(position: Int): Boolean {

        val field = dm.fields[position]
        controller.editField(context,field,position)
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
        context: Context
    ) : LinearLayoutCompat(
        context
    ) {

        var fiels: Collection<DocumentModel.FileModel> = emptyList()
            set(value){
                if(!value.isEmpty()) {
                    val tl = TableLayout(context).apply {
                        setColumnStretchable(1, true)
                        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE or
                                LinearLayout.SHOW_DIVIDER_BEGINNING or
                                LinearLayout.SHOW_DIVIDER_END
                        dividerDrawable = ColorDrawable(Color.BLACK)

                        addView(
                            FileListHeader(
                                context
                            )
                        )

                        var colored = false

                        for (r in value) {
                            addView(
                                r.toView(context).apply {
                                    setBackgroundColor(if (colored) Color.LTGRAY else Color.TRANSPARENT)
                                })
                            colored = !colored
                        }
                    }

                    removeViewAt(0)
                    addView(tl)
                }
        }

        init {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            addView(ProgressBar(context).apply {
                isIndeterminate = true
            })

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

            }
        }

        private inline fun DocumentModel.FileModel.toView(context: Context) =
            FileView(context, id, name, size)
    }

    private class FieldListView(
        context: Context,
        fields: Collection<DocumentModel.FieldModel<*>>,
        longClickListener: (Int) -> Boolean
    ) : ScrollView(
        context
    ) {

        init {

            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val card = CardView(context).apply {
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
            }

            val grid = TableLayout(context).apply {
                setColumnStretchable(1, true)
                //setColumnShrinkable(1,true)
            }
            for (fv in createViews(context, fields)) {
                fv.longClickListener = longClickListener
                grid.addView(fv)
            }

            card.addView(grid)
            addView(card)
        }

        fun allowClose() = true

        private fun createViews(
            context: Context,
            fields: Collection<DocumentModel.FieldModel<*>>
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
            fm: DocumentModel.FieldModel<*>
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
                        context, position, fm.type, fm.title, fm.isDirty, fm.value as Long?
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
                minimumHeight = dp48pixels()
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(1, getColor(R.color.colorPrimaryDark))
                }

                addView(
                    TextView(context).apply {
                        text = "$title : "
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    })

                addView(View(context).apply {
                    layoutParams = LayoutParams(
                        2,
                        LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(getColor(R.color.colorPrimaryDark))
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
                        setPadding(10)
                    }
                )
                /*addView(View(context).apply {
                    layoutParams = LayoutParams(
                        2,
                        LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.BLACK)
                })*/
                setPadding(4)
            }

            protected open fun format(v: T?): String = (v ?: "").toString()

            protected open fun getAlignment() = Gravity.START

            protected open fun createValueView(value: String, isDirty: Boolean) =
                TextView(context).apply {
                    text = value
                    gravity = getAlignment()
                    /*background = GradientDrawable().apply {
                        setColor(if (isDirty) Color.YELLOW else Color.TRANSPARENT)
                        cornerRadius = 5f
                        setStroke(1, Color.BLACK)
                    }*/
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
    }
}