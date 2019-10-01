package com.redocs.archive.ui.utils

import android.content.Context
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun showError(context: Context, ex: Exception) {
    showError(context,ex.localizedMessage)
}

fun showError(context: Context, msg: String) {
    try {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT)
            .show()
    }catch (ex: Exception){}
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

class ShortDate {

    companion object {

        var formatString: String? = null

        fun format(context: Context, v: Date): String {
            val locale = Locale.getDefault()
            var pattern = formatString
            if(formatString == null) {
                val skeleton = "dd.MM.yyyy"
                pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
            }
            return SimpleDateFormat(pattern, locale).format(v)
        }
    }
}

class LongDate {

    companion object {

        var formatString: String? = null

        fun format(context: Context, v: Date): String {
            val locale = Locale.getDefault()
            var pattern = formatString
            if(formatString == null) {
                //val use24Hour = DateFormat.is24HourFormat(context)
                val skeleton = "dd.MM.yyyy "+ DateFormat.getTimeFormat(context)//${if (use24Hour) "HH" else "hh"}:mm:ss"
                pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
            }
            return SimpleDateFormat(pattern, locale).format(v)
        }
    }
}