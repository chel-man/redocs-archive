package com.redocs.archive.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.redocs.archive.R
import com.redocs.archive.localeManager
import java.util.*


class SettingsFragment : PreferenceFragmentCompat(),PreferenceFragmentCompat.OnPreferenceStartFragmentCallback{

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val r = super.onPreferenceTreeClick(preference)
        return r
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        val fm= fragmentManager as FragmentManager
        val fragment = fm.fragmentFactory.instantiate(
            activity!!.classLoader,
            pref!!.fragment)
        fragment.arguments = pref.extras
        fragment.setTargetFragment(caller, 0)
        fm.beginTransaction()
            .replace((view?.parent as View).id,fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    override fun getCallbackFragment(): Fragment {
        return this
    }

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
            Log.d("#SF", "current activity: $activity  in $context")
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
            setPreferencesFromResource(R.xml.prefs_2,rootKey)
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
