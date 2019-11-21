package com.redocs.archive.ui.utils

import android.content.Intent

interface ActivityResultSync {
    fun setActivityResultListener(listener:(requestCode: Int, resultCode: Int, data: Intent?)->Unit)
}