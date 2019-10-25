package com.redocs.archive.ui.view.list

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.redocs.archive.ui.utils.convertDpToPixel

open class SimpleList<T>(context: Context) : RecyclerView(context){

    var model: ListModel<T> = EmptyListModel<T>()
        set(value) {
            adapter = Adapter<T>(context, value) {pos ->
                selectionListener?.invoke(pos)
            }
        }

    var selectedColor: Int = Color.GRAY

    var selectionListener: ((Int) -> Unit)? = null

    init {
        layoutManager = LinearLayoutManager(context)
        //adapter = Adapter<T>(context)
    }

    private inner class Adapter<T>(
        private val context: Context,
        private val model: ListModel<T>,
        private val selectionListener: ((Int)->Unit)?

    ) : RecyclerView.Adapter<SimpleViewHolder>(){

        private var prevSelected: View? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SimpleViewHolder =
            SimpleViewHolder(SimpleItemView(context, selectedColor))

        override fun getItemCount(): Int = model.data.size

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {

            with(holder.itemView as SimpleItemView){
                text = model.data[position].toString()

                if(prevSelected == this)
                    prevSelected = null

                if (model.selectedPosition == position)
                    prevSelected = this

                isActivated = model.selectedPosition == position

                setOnClickListener {
                    selectItem(position, it)
                }
            }
        }

        private fun selectItem(pos: Int, item: View){

            if(prevSelected != item) {
                prevSelected?.isActivated = false
                prevSelected = item
                item.isActivated = true
                model.selectedPosition = pos
                this@Adapter.selectionListener?.invoke(pos)
            }
        }

    }

    private class SimpleViewHolder(itemView: SimpleItemView) : RecyclerView.ViewHolder(itemView)

    private class SimpleItemView(
        context: Context,
        private val selectedColor: Int
    ) : LinearLayoutCompat(context){

        var text: String = ""
            set(value) {tv.text = value}

        private val tv = TextView(context)
        private var textColor: Int = tv.currentTextColor

        init {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                convertDpToPixel(48,context)
            )
            setPadding(20,0,20,0)
            orientation = VERTICAL

            with(tv){
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            addView(tv)
            addView(
                View(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,1
                    )
                    gravity = Gravity.BOTTOM
                    setBackgroundColor(Color.GRAY)
                }
            )
        }

        override fun setActivated(activated: Boolean) {
            super.setActivated(activated)
            setBackgroundColor(if(activated) selectedColor else Color.TRANSPARENT)
            tv.setTextColor(if(activated) Color.WHITE else textColor)
        }
    }

    open class ListModel<T>(
        var data: List<T>,
        var selectedPosition: Int = -1,
        var topPosition: Int = 0)

    private class EmptyListModel<T>() : ListModel<T> (emptyList())
}