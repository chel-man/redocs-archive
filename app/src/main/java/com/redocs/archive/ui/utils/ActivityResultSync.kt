package com.redocs.archive.ui.utils

import android.content.Intent

interface ActivityResultSync {
    fun listen(listener:(requestCode: Int, resultCode: Int, data: Intent?)->Unit)
}