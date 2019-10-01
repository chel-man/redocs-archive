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
import com.redocs.archive.domain.document.Document
import com.redocs.archive.domain.document.FieldType
import java.util.*
import androidx.core.view.setPadding
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.ui.events.DocumentSelectedEvent
import com.redocs.archive.ui.utils.ShortDate
import com.redocs.archive.ui.view.ActivablePanel
import java.lang.NullPointerException


class DocumentFragment() : Fragment(), EventBusSubscriber, ActivablePanel {

    override var isActive = false

    private val vm by activityViewModels<DocumentViewModel>()

    init {
        subscribe(DocumentSelectedEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is DocumentSelectedEvent -> vm.document = evt.data
        }
    }

    override fun activate() {
        createView(vm.document)
    }

    override fun deactivate() {
    }

    private fun createView(document: Document?) {

        with(view as ViewGroup) {
            try {
                removeViewAt(0)
            }catch (npe: NullPointerException){}
            if (document != null)
                addView(DocumentView(context, document))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return LinearLayoutCompat(context).apply {
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }

        }
    }


    internal open class FieldView(
        context: Context,
        private val type: FieldType,
        title: String,
        value: String?

    ) : TableRow(context)
    {
        protected open fun getAlignment() =
            when(type){
                FieldType.Integer,
                FieldType.Decimal -> Gravity.END
                else ->
                    Gravity.START
            }

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

        protected open fun createValueView(value: String?): View =
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

    class DocumentView(
        context: Context,
        doc: Document?
    ) : CardView(
        context
    ) {

        init {

            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(5)
                radius = 20F
            }
            preventCornerOverlap = true

            val grid = TableLayout(context).apply {
                setColumnStretchable(1,true)
                //setColumnShrinkable(1,true)
            }
            if(doc != null) {
                for (fv in createViews(context, doc))
                    grid.addView(fv)
            }
            addView(grid)
        }

        fun allowClose() = true

        private fun createViews(context: Context, doc: Document?): Collection<DocumentFragment.FieldView> {
            val fields = mutableListOf<DocumentFragment.FieldView>()
            doc?.let{
                for(df in it.fields)
                    fields += createFieldView(context,df)
            }
            return fields
        }

    }
}

class DocumentViewModel : ViewModel() {

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
