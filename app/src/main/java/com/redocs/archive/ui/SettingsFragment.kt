package com.redocs.archive.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.redocs.archive.R
import com.redocs.archive.localeManager
import java.util.*


class SettingsFragment : PreferenceFragmentCompat(){

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        val dsp = PreferenceManager.getDefaultSharedPreferences(context)
        val defLang = dsp.getString(localeManager.PREF_LANGUAGE_KEY,Locale.getDefault().language)
        val dl = findPreference<Preference>(localeManager.PREF_LANGUAGE_KEY)
        dl?.summary = SelectLanguageFragment.preferencesList[defLang]
    }

    class SelectLanguageFragment : PreferenceFragmentCompat() {

        companion object {
            val preferencesList = mapOf<String, String>(
                "en" to "English",
                "ru" to "Русский"
            )
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceScreen = preferenceManager
                .createPreferenceScreen(context).apply {
                    for ((key, title) in preferencesList.entries) {
                        addPreference(
                            StringPreference(
                                context,
                                "${localeManager.PREF_LANGUAGE_KEY}_$key",
                                title)
                        )
                    }
                }
        }
    }

    class Screen2Fragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.messages_preferences,rootKey)
        }

    }

    private class StringPreference(context: Context, key: String, title: String) : Preference(context){
        init {

            this.title = title
            onPreferenceClickListener = object:OnPreferenceClickListener {
                override fun onPreferenceClick(preference: Preference?): Boolean {
                    saveStringPref(context, key)
                    return true
                }

            }
        }
    }
}

@MainThread
private fun saveStringPref(context: Context, key: String){
    val kv = key.split("_")
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(kv[0], kv[1])
        .apply()

    (context as Activity).recreate()
}
