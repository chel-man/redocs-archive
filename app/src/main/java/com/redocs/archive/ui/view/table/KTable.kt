package com.redocs.archive.ui.view.table

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat

abstract class KTable<T> (
    context: Context,
    model: Model<T>
) : LinearLayoutCompat(context){

    abstract val columnNames: List<String>
    abstract val renderers: Map<Int,Renderer<*>>
    var model: Model<T> = Model(emptyList<T>())
        set(value) { loadData(value)}

    private var content: TableLayout

    abstract fun getValueAt(row: Int, col: Int)

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        orientation = VERTICAL

        addView(
            HeaderView(context)
        )

        addView(
            ScrollView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            content = TableLayout(context)
            addView(content)
        })
    }

    private fun loadData(m: Model<T>){

    }

    private inner class HeaderView(
        context:Context
    ) : TableRow(context)
    {
        init{

            for(c in columnNames) {
                addView(
                    TextView(context).apply {
                        text = c
                        gravity = Gravity.CENTER_HORIZONTAL
                    })
            }
        }
    }

    class Model<T>(
        private val data: List<T>
    )

    class Renderer<T>(
        private val context: Context
    ) {
        fun render(obj: T): View = TextView(context).apply { text = obj.toString() }
    }
}