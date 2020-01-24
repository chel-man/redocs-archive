package com.redocs.archive.framework

import android.util.Log
import com.redocs.archive.data.service.SecurityService
import com.redocs.archive.framework.net.BaseRemoteServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import security.interfaces.SecurityService as RemoteService

class SecurityServiceImpl(
    private val url: String,
    connected: Boolean
) : SecurityService, BaseRemoteServiceImpl(connected) {

    private val tz = TimeZone.getDefault().id

    override suspend fun authenticate(un: String, psw: String) =
        withContext(Dispatchers.IO) {
            prepareCall<RemoteService>(url)
                .login(un,psw,tz)
            Unit
        }

}