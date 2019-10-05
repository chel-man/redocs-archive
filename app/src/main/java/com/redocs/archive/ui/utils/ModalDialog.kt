package com.redocs.archive.ui.utils

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.redocs.archive.R

class ModalDialog() : DialogFragment() {

    private var conf: Config = EmptyConfig()

    constructor(conf: Config) : this(){
        this.conf = conf
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val d = object: AlertDialog(
            context as Context,
            false,
            null){}.apply {
            conf.title.let {
                setTitle(it)
            }

            if(conf.content != null)
                setView(conf.content)
            else
                setMessage(conf.msg)

            for(b in conf.buttons)
                setButton(b.which,resources.getString(b.title),
                    {_, _ -> conf.actionListener(b.which) })
        }

        if(conf is EmptyConfig)
            Handler().post {
                dismissAllowingStateLoss()
            }
        return d
    }

    open class Config(
        val content: View? = null,
        val msg: String? = null,
        val title: String? = null,
        val buttons: List<DialogButton> = listOf(YesButton(),NoButton()),
        val actionListener: (Int)->Unit
    )

    private class EmptyConfig() : Config(msg="I will be closed",actionListener = {})

    class SaveDialogConfig(
        content: View? = null,
        msg: String? = null,
        title: String? = null,
        buttons: List<DialogButton> = listOf(SaveButton(),CancelButton()),
        actionListener: (Int)->Unit

    ) : Config(
        content,
        msg,
        title,
        buttons,
        actionListener
    )

    abstract class DialogButton(
        @StringRes val title: Int,
        val which: Int
    ){
        companion object {
            const val POSITIVE = DialogInterface.BUTTON_POSITIVE
            const val NEGATIVE = DialogInterface.BUTTON_NEGATIVE
            const val NEUTRAL = DialogInterface.BUTTON_NEUTRAL
        }
    }

    class YesButton(
        @StringRes title: Int = R.string.dialog_yes_button_title
    ) : DialogButton(title, DialogInterface.BUTTON_POSITIVE)

    class NoButton(
        @StringRes title: Int = R.string.dialog_no_button_title
    ) : DialogButton(title, DialogInterface.BUTTON_NEGATIVE)

    class CancelButton(
        @StringRes title: Int = R.string.dialog_cancel_button_title
    ) : DialogButton(title, DialogInterface.BUTTON_NEUTRAL)

    class SaveButton(
        @StringRes title: Int = R.string.dialog_save_button_title
    ) : DialogButton(title, DialogInterface.BUTTON_POSITIVE)

}