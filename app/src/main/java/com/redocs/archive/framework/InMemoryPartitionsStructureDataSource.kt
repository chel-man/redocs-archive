package com.redocs.archive.framework

import com.redocs.archive.data.partitions.PartitionsStructureDataSource
import com.redocs.archive.domain.partitions.PartitionStructureNode
import kotlinx.coroutines.*

class InMemoryPartitionsStructureDataSource: PartitionsStructureDataSource {

    private lateinit var nodes: List<Node>

    init {
        GlobalScope.launch(context=Dispatchers.Default){
            nodes=listOf(
                Node(PartitionStructureNode(0, "Электронный архив",false),mutableListOf(
                    Node(PartitionStructureNode(-1, "Корзина",false), mutableListOf()),
                    Node(PartitionStructureNode(1, "Раздел 1",true),mutableListOf()),
                    Node(PartitionStructureNode(2, "Раздел 2",false),mutableListOf(
                        Node(PartitionStructureNode(22, "Раздел 22",true),mutableListOf())))

                )))
        }
    }

    override suspend fun get(id: Long): PartitionStructureNode =
        withContext(Dispatchers.Default) {
            delay(1000)
            getNodeById(nodes,id)!!.node
        }

    override suspend fun getChildren(id: Long): List<PartitionStructureNode> =

        withContext(Dispatchers.Default) {
            delay(1000)

            val l= mutableListOf<PartitionStructureNode>()
            for(n in getNodeById(nodes,id)!!.children){
                l.add(n.node)
            }
            l
        }

    override suspend fun add(parentId: Long, name: String, after: Long): PartitionStructureNode {
        TODO("not implemented") //To change body of created functions use FileInfo | Settings | FileInfo Templates.
    }

}

data class Node(val node:PartitionStructureNode, val children: List<Node>)

private fun getNodeById(nodes: List<Node>, id: Long, level: Int=0): Node? {
    for(n in nodes){
        if(n.node.id==id)
            return n
        var n=getNodeById(n.children,id,level+1)
        if(n !=null)
            return n
    }
    if(level==0)
        throw Exception("Раздел с ID=$id не найден")
    return null
}