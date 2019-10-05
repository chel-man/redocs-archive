package com.redocs.archive

import android.content.Context
import android.graphics.PorterDuff
import android.os.Handler
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

fun EditText.setFocusAndShowKeyboard(){
    requestFocus()
    Handler().postDelayed({
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    },200)
}

fun Any.asLongOrNull(): Long? =
    (this as? Long) ?: (this as Int)?.toLong()

fun Any.asLongOrOriginal(): Any {

    var v = this as? Long
    if(v != null) return v
    if((this as? Int) != null) return (this as Int).toLong()
    return this

}