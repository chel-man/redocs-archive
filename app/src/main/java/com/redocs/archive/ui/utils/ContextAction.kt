package com.redocs.archive.ui.utils

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.view.ActionMode

interface ContextActionSource {

    val lockContent: Boolean

    fun createContextActionMenu(mode: ActionMode,inflater: MenuInflater, menu: Menu)
    fun onDestroyContextAction()
    fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean
}