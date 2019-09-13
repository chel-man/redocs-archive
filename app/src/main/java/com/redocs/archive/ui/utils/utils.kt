package com.redocs.archive.ui.utils

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.Toast

fun showError(context: Context, ex: Exception) {
    showError(context,ex.localizedMessage)
}

fun showError(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT)
        .show()
}

private var metrics: DisplayMetrics? = null

fun getMetrix(context: Context): DisplayMetrics {
    return metrics ?: context.resources.displayMetrics
}

fun convertDpToPixel(dp: Int, context: Context): Int {
    return dp * getMetrix(context).scaledDensity.toInt()
}

fun spToPx(sp: Float, context: Context): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getMetrix(context)).toInt()
}