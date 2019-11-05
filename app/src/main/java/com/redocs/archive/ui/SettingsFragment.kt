package com.redocs.archive.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.redocs.archive.R


class SettingsFragment : PreferenceFragmentCompat()/*,PreferenceFragmentCompat.OnPreferenceStartFragmentCallback*/{

    /*override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
        pref: Preference?
    ): Boolean {
        val fm= fragmentManager as FragmentManager
        val fragment = fm.fragmentFactory.instantiate(
            activity!!.classLoader,
            pref!!.fragment)
        fragment.arguments = pref.extras
        fragment.setTargetFragment(caller, 0)
        // Replace the existing Fragment with the new Fragment
        fm.beginTransaction()
            .remove(this)
            .add(fragment,"${fragment::class.java.name}")
            .addToBackStack(null)
            .commit()
        return true
    }*/

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val fn = preference?.extras?.getString("fragment_name")
        Log.d("#SF",fn)
        val fm= fragmentManager as FragmentManager
        val fragment = fm.fragmentFactory.instantiate(
            activity!!.classLoader,
            fn!!)
        // Replace the existing Fragment with the new Fragment
        fm.beginTransaction()
            //.remove(this)
            //.add(fragment,"${fragment::class.java.name}")
            .replace((view?.parent as View).id,fragment)
            .addToBackStack(null)
            .commit()
        return true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat?,
        pref: PreferenceScreen?
    ): Boolean {

        /*fragmentManager?.fragmentFactory?.instantiate(
            Thread.currentThread().contextClassLoader,
            pref!!.fragment::class.java.name)?.run {*/
            fragmentManager?.beginTransaction()
                ?.remove(this@SettingsFragment)
                ?.add(Screen2SettingsFragment(), "Screen1SettingsFragment")
                ?.addToBackStack(null)
                ?.commit()
        //}
        return true
    }

}

class Screen1SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_1,rootKey)
    }

}

class Screen2SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_2)
    }

}