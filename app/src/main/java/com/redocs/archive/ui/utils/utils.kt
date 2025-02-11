package com.redocs.archive.ui.utils

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

fun showError(context: Context, ex: Throwable) {
    showError(context,ex.localizedMessage ?: "$ex")
    Log.e("#ERROR","",ex)
}

fun showError(context: Context, msg: String) {
    try {
        Toast.makeText(context, msg, Toast.LENGTH_LONG)
            .show()
    }catch (ex: Exception){}
}

fun causeException(ex: Throwable): Throwable {
    var c = ex
    while(true){
        val cc = c.cause ?: break
        c=cc
    }

    return c
}

private var metrics: DisplayMetrics? = null

fun getMetrix(context: Context): DisplayMetrics {
    return metrics ?: context.resources.displayMetrics
}

fun convertDpToPixel(dp: Int, context: Context): Int {
    return dp * getMetrix(context).scaledDensity.toInt()
}

fun convertSpToPx(sp: Float, context: Context): Int {
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

enum class Action {
    VIEW,
    DELETE,
    ADD
}

class LocaleManager {

    companion object {
        const val PREF_LANGUAGE_KEY = "language"
        val instance = LocaleManager()
    }

    fun getLocalizedContext(context: Context?): Context? {
        return setLocale(context)
    }

    private fun setLocale(context: Context?): Context? =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_LANGUAGE_KEY, Locale.getDefault().language)?.let {
                updateResources(context, it)
            }

    private fun updateResources(context: Context?, language: String): Context? {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val res = context?.resources
        val config = Configuration(res?.configuration)
        config.setLocale(locale)
        return context?.createConfigurationContext(config);
    }
}