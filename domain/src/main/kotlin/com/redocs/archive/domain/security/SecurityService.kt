package com.redocs.archive.domain.security

interface SecurityService {
    suspend fun authenticate(name: String, pwd: String)
}