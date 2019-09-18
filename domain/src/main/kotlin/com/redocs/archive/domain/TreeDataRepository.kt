package com.redocs.archive.domain

interface TreeDataRepositoryInterface<T : TreeNode> {

    suspend fun getChildren(id: Long): List<T>
    suspend fun get(id: Long): T
    suspend fun getPath(id: Long): List<Long>
}