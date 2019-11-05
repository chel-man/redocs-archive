package com.redocs.archive.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.get
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.redocs.archive.R


class SettingsFragment : PreferenceFragmentCompat(),PreferenceFragmentCompat.OnPreferenceStartFragmentCallback{

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
    }

    class Screen1Fragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_1,rootKey)
        }

    }

    class Screen2Fragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_2,rootKey)
        }

    }}

