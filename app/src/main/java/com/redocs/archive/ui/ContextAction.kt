package com.redocs.archive.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.view.ActionMode

interface ContextActionSource {

    val lockContent: Boolean

    fun createContextActionMenu(inflater: MenuInflater, menu: Menu)
    fun onDestroyContextAction()
    fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean
}