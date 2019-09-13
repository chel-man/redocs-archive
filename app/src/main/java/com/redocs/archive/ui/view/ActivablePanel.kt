package com.redocs.archive.ui.view

interface ActivablePanel {
    var isActive: Boolean
    fun activate()
    fun deactivate()
}