package com.redocs.archive.ui.utils

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.widget.DatePicker
import android.widget.EditText
import com.redocs.archive.asDoubleOrNull
import com.redocs.archive.asLongOrNull
import com.redocs.archive.setFocusAndShowKeyboard
import java.util.*

interface CustomEditor<T> {
    val value: T?
}

abstract class AbstractCustomEditor<T>(
    context: Context,
    value: T?
) : EditText(context), CustomEditor<T>{

    abstract override val value: T?

    init{
        setText(format(value))
        setFocusAndShowKeyboard()
    }

    protected open fun format(v: T?): String = (v ?: "").toString()
}

class TextCustomEditor(
    context: Context,
    value: String
) : AbstractCustomEditor<String>(context,value){

    override val value: String
        get() = if(text == null) "" else text.toString()

    var minLength: Int = 0
        set(value){

            if(value >0 )
                addTextChangedListener(object:TextWatcher{
                    override fun afterTextChanged(s: Editable?) {
                        if(s.toString().length < value)
                            error = "Min len is $value"
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    }
                })
            field = value
        }
}

class IntegerCustomEditor(
    context: Context,
    value: Long?
) : AbstractCustomEditor<Long>(context,value){

    override val value: Long?
        get()  = text?.toString()?.toLongOrNull()

    //override fun format(v: Long?): String = (v ?: "").toString()

    init {
        inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_SIGNED
    }
}

class DecimalCustomEditor(
    context: Context,
    value: Double?
) : AbstractCustomEditor<Double>(context,value){

    override val value: Double?
        get()  = text?.toString()?.toDoubleOrNull()

    //override fun format(v: Long?): String = (v ?: "").toString()

    init {
        inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
    }
}

class DateEditor(
    context: Context, value: Date?
) : DatePicker(
    context
), CustomEditor<Date> {

    override val value: Date?
        get() =
            Calendar.getInstance().apply {
                this.set(year,month,dayOfMonth)
            }.time
}