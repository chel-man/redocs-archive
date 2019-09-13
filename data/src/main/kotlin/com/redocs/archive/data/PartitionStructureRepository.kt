package com.redocs.archive.data

import com.redocs.archive.domain.TreeDataRepositoryInterface
import com.redocs.archive.domain.TreeNode


abstract class PartitionStructureRepository<T: TreeNode>(
    protected var partitionStructureDataSource: PartitionStructureDataSource<T>
) : TreeDataRepositoryInterface<T> {

    override suspend fun getChildren(id: Int): List<T> {
        return partitionStructureDataSource.getChildren(id)
    }
    override suspend fun get(id: Int): T {
        return partitionStructureDataSource.get(id)
    }

    override suspend fun getPath(id: Int): List<Int> {
        return partitionStructureDataSource.getPath(id)
    }

}