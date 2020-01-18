package com.redocs.archive.framework

import com.redocs.archive.domain.security.SecurityService
import com.redocs.archive.framework.net.RemoteServiceProxyFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class SecurityServiceImpl(
    private val url: String
) : SecurityService {

    override suspend fun authenticate(un: String, psw: String) =
        withContext(Dispatchers.IO) {
            RemoteServiceProxyFactory.log = true
            RemoteServiceProxyFactory
                .create<SecurityService>(url)
                .authenticate(un,psw)
        }

}