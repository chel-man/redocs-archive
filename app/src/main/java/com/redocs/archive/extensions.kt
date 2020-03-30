package com.redocs.archive

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Handler
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.redocs.archive.ui.utils.LocaleManager
import com.redocs.archive.ui.utils.convertDpToPixel

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

fun TextView.textUnderline() {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
}

fun TextView.textBold() {
    typeface = Typeface.DEFAULT_BOLD
}

fun Any.asLongOrNull(): Long? =
    (this as? Long) ?: (this as? Int)?.toLong()

fun Any.asDoubleOrNull(): Double? =
    (this as? Double) ?: (this as? Float)?.toDouble()

fun Any.asLongOrOriginal(): Any {

    val v = this as? Long
    if(v != null) return v
    if((this as? Int) != null) return this.toLong()
    return this

}

fun View.getColor(@ColorRes color: Int): Int {
    return ContextCompat.getColor(
            context, color
        )
}

fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

fun View.dp48pixels() = convertDpToPixel(48,context)

val Activity.localeManager get() = LocaleManager.instance
val Fragment.localeManager get() = LocaleManager.instance

fun View.hideKeyboard(){
    //Handler().postDelayed({
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(this.windowToken, 0)
    //},200)
}
