package com.redocs.archive.ui.view.panels

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.redocs.archive.R
import com.redocs.archive.addRipple
import com.redocs.archive.ui.utils.ActivablePanel
import com.redocs.archive.ui.utils.convertDpToPixel

class StackPanel(
    context: Context?
) : LinearLayoutCompat(
    context
){
    private val titles: LinearLayoutCompat
    private var activePosition = 0
    private val panels = mutableListOf<Panel>()
    private val dp48 = convertDpToPixel(48, context as Context)

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        orientation = VERTICAL

        titles = LinearLayoutCompat(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            orientation = VERTICAL

            setBackgroundColor(
                ContextCompat.getColor(
                    context as Context, R.color.colorPrimaryDark
                )
            )
        }
        addView(titles)
    }

    fun addPanel(title: String, content: View){

        val position = panels.size
        panels += Panel(title, content)

        content.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            1F
        )

        if(position == 0)
            addView(content,0)
        else{
            titles.addView(
                TextView(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        dp48
                    )
                    setTextColor(Color.WHITE)
                    text = title
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
                    addRipple()
                    setOnClickListener {
                        activatePanel(position, it as TextView)
                    }
                }
            )
            titles.addView(
                View(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        5
                    )
                    setBackgroundColor(Color.WHITE)
                }
            )
        }
    }

    private fun activatePanel(position: Int, tv: TextView){

        with(tv){
            val pos = activePosition
            text = panels[pos].title
            setOnClickListener {
                activatePanel(pos, this)
            }
        }
        activePosition = position
        removeViewAt(0)
        val content = panels[position].content
        addView(content,0)
        (content as? ActivablePanel)?.activate()

    }

    private data class Panel(
        val title: String,
        val content: View
    )
}