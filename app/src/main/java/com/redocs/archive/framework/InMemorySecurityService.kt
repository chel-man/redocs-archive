package com.redocs.archive.framework

import com.redocs.archive.domain.security.SecurityService
import java.lang.Exception

class InMemorySecurityService : SecurityService {

    override suspend fun authenticate(un: String, psw: String) {
    }
}