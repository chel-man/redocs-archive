package com.redocs.archive.ui.view.list

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class SimpleList<T>(context: Context) : RecyclerView(context){

    var data: List<T> = emptyList()
        set(value) {
            adapter = Adapter<T>(context, value, {pos ->
                selectionListener?.invoke(pos)
            })
        }

    var selectionListener: ((Int) -> Unit)? = null

    init {
        layoutManager = LinearLayoutManager(context)
        //adapter = Adapter<T>(context)
    }

    private class Adapter<T>(
        private val context: Context,
        private val data: List<T>,
        private val selectionListener: ((Int)->Unit)?

    ) : RecyclerView.Adapter<SimpleViewHolder>(){

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SimpleViewHolder =
            SimpleViewHolder(SimpleItemView(context))

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
            with(holder.itemView as TextView){
                text = data[position].toString()

                setOnClickListener {
                    this@Adapter.selectionListener?.invoke(position)
                }
            }
        }

    }

    private class SimpleViewHolder(itemView: SimpleItemView) : RecyclerView.ViewHolder(itemView)

    private class SimpleItemView(context: Context) : TextView(context)
}