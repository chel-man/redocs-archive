package com.redocs.usecases.partitions

import com.redocs.archive.data.partitions.PartitionsStructureRepository
import com.redocs.archive.domain.partitions.PartitionStructureNode

class GetNodeUseCase(private val repo: PartitionsStructureRepository) {

    suspend operator fun invoke(id: Int): PartitionStructureNode{
        return repo.get(id)
    }
}