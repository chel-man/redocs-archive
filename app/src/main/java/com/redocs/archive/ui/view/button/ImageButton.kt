package com.redocs.archive.ui.view.button

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.redocs.archive.addRipple
import com.redocs.archive.ui.utils.convertDpToPixel

open class ImageButton(
    context: Context,
    size: Int
) : ImageButton(context){

    init {
        addRipple()
        val p = convertDpToPixel(size, context)
        layoutParams = ViewGroup.LayoutParams(p,p)
        scaleType = ImageView.ScaleType.CENTER
    }

    fun setIcon(@DrawableRes drawable: Int, tintColor: Int) {
        setImageDrawable(
            AppCompatResources.getDrawable(
                context,drawable
            )?.apply {
                DrawableCompat.setTint(
                    this, tintColor)
            }
        )
    }
}

class ImageButton48(
    context: Context
) : com.redocs.archive.ui.view.button.ImageButton(context,48)