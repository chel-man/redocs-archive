package com.redocs.archive.framework

import com.redocs.archive.data.service.SecurityService
import com.redocs.archive.framework.net.BaseRemoteServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import security.interfaces.SecurityService as RemoteSecurityService

class SecurityServiceImpl(
    private val url: String,
    connected: Boolean
) : SecurityService, BaseRemoteServiceImpl(connected) {


    override suspend fun authenticate(un: String, psw: String) =
        withContext(Dispatchers.IO) {
            prepareCall(RemoteSecurityService::class.java,url)
                .login(un,psw)
            Unit
        }

}