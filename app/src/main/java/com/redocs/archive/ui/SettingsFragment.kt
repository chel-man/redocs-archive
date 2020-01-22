package com.redocs.archive.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.MainThread
import androidx.preference.*
import com.redocs.archive.R
import com.redocs.archive.ui.utils.LocaleManager.Companion.PREF_LANGUAGE_KEY
import java.util.*


class SettingsFragment : PreferenceFragmentCompat(){

    companion object {
        const val SERVICE_URL_KEY = "service-url"
        val restartablePreferenceKeys = listOf<String>(
            PREF_LANGUAGE_KEY,
            SERVICE_URL_KEY,
            Settings.Global.NETWORK_PREFERENCE
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        if(NetworkFragment.preferencesList.isEmpty()) {
            context?.resources?.apply {
                getStringArray(R.array.settings_network_item_keys).apply {
                    val keys = this
                    getStringArray(R.array.settings_network_item_titles).apply {
                        for ((i, key) in keys.asList().withIndex()) {
                            NetworkFragment.preferencesList[key] = this[i]
                        }
                    }
                }
            }
        }

        addPreferencesFromResource(R.xml.preferences)
        val dsp = PreferenceManager.getDefaultSharedPreferences(context)

        findPreference<Preference>(PREF_LANGUAGE_KEY)?.apply {
            val defLang = dsp
                    .getString(PREF_LANGUAGE_KEY,Locale.getDefault().language)
            summary = SelectLanguageFragment.preferencesList[defLang]
        }

        findPreference<Preference>("network")?.apply {
            val pval = dsp
                .getString(
                    Settings.Global.NETWORK_PREFERENCE,
                    MainActivity.NetworkStateReceiver.ANY)
            summary = NetworkFragment.preferencesList[pval]
        }

        preferenceScreen.addPreference(
            EditTextPreference(context).apply {
                key = SERVICE_URL_KEY
                title = resources.getString(R.string.settings_service_url_title)
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            })
    }

    class SelectLanguageFragment : PreferenceListFragment() {

        companion object {
            val preferencesList = mapOf<String, String>(
                "en" to "English",
                "ru" to "Русский"
            )
        }

        override val prefix = PREF_LANGUAGE_KEY

        override val preferences: Map<String, String> by lazy {
            preferencesList
        }
    }

    class NetworkFragment : PreferenceListFragment() {

        companion object {
            val preferencesList = mutableMapOf<String,String>()
        }

        override val prefix = "network"
        override val preferences: Map<String, String> by lazy {
            preferencesList
        }
    }

    abstract class PreferenceListFragment : PreferenceFragmentCompat() {

        abstract val prefix: String
        abstract val preferences: Map<String,String>

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            preferenceScreen = preferenceManager
                .createPreferenceScreen(context).apply {
                    for ((key, title) in preferences.entries) {
                        addPreference(
                            StringPreference(
                                context,
                                "${prefix}_$key",
                                title)
                        )
                    }
                }
        }
    }

    /*class Screen2Fragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.messages_preferences,rootKey)
        }

    }*/

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
