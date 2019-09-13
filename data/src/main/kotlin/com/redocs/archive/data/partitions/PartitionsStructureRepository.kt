package com.redocs.archive.data.partitions

import com.redocs.archive.data.PartitionStructureRepository
import com.redocs.archive.data.PartitionStructureDataSource
import com.redocs.archive.domain.partitions.PartitionStructureNode

class PartitionsStructureRepository(
    private var partitionsStructureDataSource: PartitionsStructureDataSource
): PartitionStructureRepository<PartitionStructureNode>(partitionsStructureDataSource)

interface PartitionsStructureDataSource: PartitionStructureDataSource<PartitionStructureNode>
