package com.redocs.archive.data.service

interface SecurityService {
    suspend fun authenticate(un: String, psw: String)
}