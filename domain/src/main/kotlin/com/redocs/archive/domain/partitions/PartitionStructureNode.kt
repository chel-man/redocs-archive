package com.redocs.archive.domain.partitions

import com.redocs.archive.domain.TreeNode

class PartitionStructureNode(
    id: Long,
    name: String,
    isLeaf: Boolean = true
): TreeNode(id, name, isLeaf)