package com.redocs.archive.data

import com.redocs.archive.domain.TreeNode

interface PartitionStructureDataSource<T: TreeNode> {
    suspend fun getChildren(id: Long): List<T>
    suspend fun get(id: Long): T
    suspend fun add(parentId: Long, name: String, after: Long = Long.MIN_VALUE): T
    suspend fun getPath(id: Long): List<Long> {
        return listOf()
    }
}