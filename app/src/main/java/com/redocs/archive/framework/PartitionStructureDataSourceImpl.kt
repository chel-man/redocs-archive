package com.redocs.archive.framework

import com.redocs.archive.data.partitions.PartitionsStructureDataSource
import com.redocs.archive.domain.partitions.PartitionStructureNode
import com.redocs.archive.framework.net.RemoteServiceProxyFactory
import commons.api.container.Container
import commons.api.services.ClassificatorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartitionStructureDataSourceImpl(
    private val baseUrl: String
) : PartitionsStructureDataSource
{

    override suspend fun getChildren(id: Long): List<PartitionStructureNode> =
        withContext(Dispatchers.IO) {
            val l = mutableListOf<PartitionStructureNode>()
            RemoteServiceProxyFactory
                .create<ClassificatorService>("$baseUrl/classificator").apply {
                    (getContainerChildrenAsync(
                        get(id)
                    ) as PromiseImpl).apply {
                        onPartial { l.add(it.toPartitionStructureNode()) }
                        wait()
                    }
                }
            l
        }

    override suspend fun get(id: Long): PartitionStructureNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun add(parentId: Long, name: String, after: Long): PartitionStructureNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

private fun Container.toPartitionStructureNode(): PartitionStructureNode =
    PartitionStructureNode(id,name,childCount == 0)
