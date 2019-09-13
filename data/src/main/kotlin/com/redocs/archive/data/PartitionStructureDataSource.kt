package com.redocs.archive.data

import com.redocs.archive.domain.TreeNode

interface PartitionStructureDataSource<T: TreeNode> {
    suspend fun getChildren(id: Int): List<T>
    suspend fun get(id: Int): T
    suspend fun add(parentId: Int, name: String, after: Int = Int.MIN_VALUE): T
    suspend fun getPath(id: Int): List<Int> {
        return listOf()
    }
}