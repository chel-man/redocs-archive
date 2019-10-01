package com.redocs.archive.ui

import android.graphics.PorterDuff
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment

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

fun Fragment.isParentOf(v: View): Boolean {

    var p = v.parent
    while(p != null){
        if(p as? View == view)
            return true
        p = p.parent
    }
    return false

}