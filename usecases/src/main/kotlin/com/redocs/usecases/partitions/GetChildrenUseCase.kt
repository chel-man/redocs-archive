package com.redocs.usecases.partitions

import com.redocs.archive.data.partitions.PartitionsStructureRepository
import com.redocs.archive.domain.partitions.PartitionStructureNode

class GetChildrenUseCase (private val repo: PartitionsStructureRepository) {

    suspend operator fun invoke(id: Int): List<PartitionStructureNode> {
        return repo.getChildren(id)
    }
}