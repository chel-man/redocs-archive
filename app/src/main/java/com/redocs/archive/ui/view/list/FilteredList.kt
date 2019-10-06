package com.redocs.archive.ui.view.list

import android.content.Context
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.widget.LinearLayoutCompat
import com.redocs.archive.setFocusAndShowKeyboard

class FilteredList<T> (
    context: Context,
    private val data: List<T>,
    value: T? = null
) : LinearLayoutCompat(context){

    var selectionListener: (()->Unit)? = null

    val selected: T?
        get() = data[position]

    var text: String? = null

    private var position = -1
    private val filtered: MutableList<T> = mutableListOf()
    private val list = SimpleList<T>(context)

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            orientation = VERTICAL
        }

        addView(
            EditText(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )

                value?.let {
                    setText(it.toString())
                }

                addTextChangedListener(object:TextWatcher {
                    override fun afterTextChanged(s: Editable?) = Unit

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int,after: Int) = Unit

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                        setFilter(this@apply.text.toString())
                    }

                })

                setFocusAndShowKeyboard()
            }
        )

        addView(
            list.apply {
                selectionListener = {position: Int ->
                    this@FilteredList.position = position
                    this@FilteredList.selectionListener?.invoke()
                }

                Handler().post {
                    if(value != null)
                        setFilter(value.toString())
                    else
                        data = data
                }
            }
        )
    }

    private fun setFilter(text: String){

        val filter = text.toLowerCase()
        val flen = filter.length

        filtered.clear()
        this@FilteredList.text = text
        position = -1

        for(m in data){
            val ms = m.toString().toLowerCase()
            if(ms.length <= flen && ms.contains(filter))
                filtered += m
        }

        list.data = filtered

    }
}