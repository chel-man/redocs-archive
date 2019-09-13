package com.redocs.archive.ui

import android.graphics.PorterDuff
import android.view.Menu
import androidx.annotation.ColorInt
import com.redocs.archive.MessageType
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber

fun Menu.setItemEnabled(id: Int, enabled: Boolean, @ColorInt colorId: Int) {

    findItem(id).apply {
        isEnabled = enabled
        val ic = icon.mutate()
        if (isEnabled)
            ic.clearColorFilter()
        else
            ic.setColorFilter(colorId,PorterDuff.Mode.SRC_ATOP)
    }
}