package com.redocs.archive.ui.view.list

import android.content.Context
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.widget.LinearLayoutCompat
import com.redocs.archive.setFocusAndShowKeyboard

open class FilteredList<T> (
    context: Context,
    private val value: T? = null
) : LinearLayoutCompat(context){

    var selectionListener: (()->Unit)? = null
    val selected: T?
        get() = (data as List<T>)[position]

    var text: String? = null
    var data: List<T> = emptyList()
        set(value) {
            loadDataToList(value as List<T>)
            field = value
        }

    private var position = -1
    private val filtered: MutableList<T> = mutableListOf()
    private var list: SimpleList<T>? = null

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
            ProgressBar(context).apply {
                this.isIndeterminate = true
            }
        )
    }

    private fun loadDataToList(data: List<T>){

        if(list == null){
            list = SimpleList<T>(context).apply {
                selectionListener = {position: Int ->
                    this@FilteredList.position = position
                    this@FilteredList.selectionListener?.invoke()
                }

                Handler().post {
                    this.data = data
                    if(value != null)
                        setFilter(value.toString())
                }
            }

            removeViewAt(1)
            addView(list)
        }
        else
            list?.data = data

    }

    private fun setFilter(text: String){

        val filter = text.toLowerCase().trim()
        val flen = filter.length

        filtered.clear()
        this@FilteredList.text = text
        position = -1
        for(m in data){
            val ms = m.toString().toLowerCase()
            if(ms.length >= flen && ms.contains(filter))
                filtered += m
        }

        (list as SimpleList<T>).data = filtered

    }
}