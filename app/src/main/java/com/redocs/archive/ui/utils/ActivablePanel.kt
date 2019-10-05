package com.redocs.archive.ui.utils

interface ActivablePanel {
    var isActive: Boolean
    fun activate()
    fun deactivate()
}