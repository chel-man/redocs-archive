package com.redocs.archive.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redocs.archive.domain.document.DataType
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import org.w3c.dom.Text
import java.util.*
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.setPadding


class DocumentFragment() : Fragment() {

    private lateinit var doc: Document
    private var model: DocumentModel? = null
    private val vm by activityViewModels<DocumentViewModel>()

    constructor(doc: Document) : this() {
        this.doc = doc
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val dm = model ?: createDocumentModel(context as Context,doc)
        vm.model = dm

        return DocumentView(context as Context,dm)
    }

    data class DocumentModel(
        val id: Long,
        val fields: Collection<FieldView<*>>
    )

    abstract class FieldView<T>(
        context: Context,
        val title: String,
        val value: T?

    ) : TableRow(context)
    {
        abstract val type: FieldType
        protected open fun getAlignment(): Int = Gravity.START

        init {

            addView(
                TextView(context).apply {
                    text = "$title : "
                    gravity = Gravity.END
                })
            addView(
                TextView(context).apply {
                    text = "$value"
                    //textAlignment = alignment
                    gravity = getAlignment()
                    background = GradientDrawable().apply {
                        setColor(Color.TRANSPARENT) // Changes this drawbale to use a single color instead of a gradient
                        cornerRadius = 5f
                        setStroke(1, Color.BLACK)}
                    //setPadding(8)
                    layoutParams = generateDefaultLayoutParams().apply {
                        setMargins(0,0,10,0)
                    }
                })
            setPadding(4)
        }
    }

    class TextFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<String>(
        context as Context,
        df.title,
        df.value as String?
    ){
        override val type = FieldType.Text
    }

    class IntFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<Int>(
        context as Context,
        df.title,
        df.value as Int
    ){
        override val type = FieldType.Integer
        override fun getAlignment() = Gravity.END
    }

    class DecimalFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<Double>(
        context as Context,
        df.title,
        df.value as Double
    ){
        override val type = FieldType.Decimal
        override fun getAlignment() = Gravity.END
    }

    class DateFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<Date>(
        context as Context,
        df.title,
        df.value as Date?
    ){
        override val type = FieldType.Text
    }

    class LongTextFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<String>(
        context as Context,
        df.title,
        df.value as String?
    ){
        override val type = FieldType.Text
    }

    class DictionaryFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<String>(
        context as Context,
        df.title,
        df.value as String?
    ){
        override val type = FieldType.Text
    }

    class MVDictionaryFieldView(
        context: Context?,
        df: Document.Field

    ) : FieldView<String>(
        context as Context,
        df.title,
        df.value as String?
    ){
        override val type = FieldType.Text
    }

    class DocumentView(
        context: Context,
        private val model: DocumentModel
    ) : LinearLayoutCompat(
        context
    ) {

        constructor(context: Context, doc: Document) :
                this(context,createDocumentModel(context,doc))

        init {

            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }
            //setBackgroundColor(Color.LTGRAY)

            val cardView =CardView(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(5)
                    radius = 20F
                }
                preventCornerOverlap = true
            }

            val grid = TableLayout(context).apply {
                setColumnStretchable(1,true)
                //setColumnShrinkable(1,true)
            }
            val m = model
            for(fv in m.fields)
                grid.addView(fv)
            cardView.addView(grid)
            addView(cardView)
        }
    }
}

class DocumentViewModel : ViewModel() {

    val coroScope= viewModelScope
    var model: DocumentFragment.DocumentModel? = null
    var topField = 0
}

private fun createDocumentModel(context: Context,doc: Document): DocumentFragment.DocumentModel {
    val fields = mutableListOf<DocumentFragment.FieldView<*>>()
    for(df in doc.fields)
        fields += createFieldView(context,df)
    val dm = DocumentFragment.DocumentModel(doc.id, fields)
    return dm
}

private fun createFieldView(context: Context,df: Document.Field): DocumentFragment.FieldView<*> =
    when(df.type){
        FieldType.Text -> DocumentFragment.TextFieldView(context, df)
        FieldType.Integer -> DocumentFragment.IntFieldView(context, df)
        FieldType.Decimal -> DocumentFragment.DecimalFieldView(context, df)
        FieldType.Date -> DocumentFragment.DateFieldView(context, df)
        FieldType.LongText-> DocumentFragment.LongTextFieldView(context, df)
        FieldType.Dictionary -> DocumentFragment.DictionaryFieldView(context, df)
        FieldType.MVDictionary -> DocumentFragment.MVDictionaryFieldView(context, df)
        else ->
            throw ClassNotFoundException("Field of type ${df.type} not found")
    }

