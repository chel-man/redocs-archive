package com.redocs.archive.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.R
import com.redocs.archive.hideKeyboard
import com.redocs.archive.ui.utils.causeException
import com.redocs.archive.ui.utils.showError
import kotlinx.coroutines.runBlocking

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return context?.let { LoginView(it) }
    }
}

class LoginView(context: Context) : LinearLayoutCompat(context) {

    init {

        val un = EditText(context)
        val psw = EditText(context)
        addView(un)
        addView(psw)
        addView(Button(context).apply {
            text = "Login"
            setOnClickListener {
                isEnabled = false
                (context as Activity).currentFocus.hideKeyboard()
                runBlocking {
                    try {
                        ArchiveApplication.securityService.authenticate(
                            un.text.toString(), psw.text.toString())

                        findNavController().apply {
                            popBackStack(R.id.login_nav_dest, true)
                            Handler().post {
                                navigate(R.id.home_nav_dest)
                            }
                        }
                    } catch (ex: Exception) {
                        val sex = causeException(ex)
                        Log.e("#LOGIN","$sex",sex)
                        showError(context, sex)
                        isEnabled = true
                    }
                }
            }
        })
    }
}
