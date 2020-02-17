package com.redocs.archive.framework

import com.redocs.archive.data.partitions.PartitionsStructureDataSource
import com.redocs.archive.domain.partitions.PartitionStructureNode
import com.redocs.archive.framework.net.BaseRemoteServiceImpl
import commons.api.container.Container
import commons.api.services.ClassificatorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartitionStructureDataSourceImpl(
    private val url: String,
    connected: Boolean
) : PartitionsStructureDataSource, BaseRemoteServiceImpl(connected)
{

    override suspend fun getChildren(id: Long): List<PartitionStructureNode> =
        withContext(Dispatchers.IO) {
            val l = mutableListOf<PartitionStructureNode>()
            (getService<ClassificatorService>(url).apply {
                getContainerChildrenAsync(
                    get(id))
                .onPartial {
                    l.add(
                        it.toPartitionStructureNode()) }
            } as PromiseImpl<*,*>)
            .wait()
            l
        }

    override suspend fun get(id: Long): PartitionStructureNode =
        withContext(Dispatchers.IO) {
            getService<ClassificatorService>(url)
                .get(id)
                .toPartitionStructureNode()
        }

    override suspend fun add(parentId: Long, name: String, after: Long): PartitionStructureNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

private fun Container.toPartitionStructureNode(): PartitionStructureNode =
    PartitionStructureNode(id,name,childCount == 0)
