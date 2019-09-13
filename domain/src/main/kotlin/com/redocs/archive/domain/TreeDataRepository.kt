package com.redocs.archive.domain

interface TreeDataRepositoryInterface<T : TreeNode> {

    suspend fun getChildren(id: Int): List<T>
    suspend fun get(id: Int): T
    suspend fun getPath(id: Int): List<Int>
}