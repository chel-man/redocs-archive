package com.redocs.archive.data

import com.redocs.archive.domain.TreeDataRepositoryInterface
import com.redocs.archive.domain.TreeNode


abstract class PartitionStructureRepository<T: TreeNode>(
    protected var partitionStructureDataSource: PartitionStructureDataSource<T>
) : TreeDataRepositoryInterface<T> {

    override suspend fun getChildren(id: Long): List<T> {
        return partitionStructureDataSource.getChildren(id)
    }
    override suspend fun get(id: Long): T {
        return partitionStructureDataSource.get(id)
    }

    override suspend fun getPath(id: Long): List<Long> {
        return partitionStructureDataSource.getPath(id)
    }

}