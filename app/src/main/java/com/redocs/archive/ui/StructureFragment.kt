package com.redocs.archive.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.data.partitions.PartitionsStructureRepository
import com.redocs.archive.framework.EventBus
import com.redocs.archive.ui.events.ActivateDocumentListEvent
import com.redocs.archive.ui.events.PartitionNodeSelectedEvent
import com.redocs.archive.ui.view.partitions.Action
import com.redocs.archive.ui.view.partitions.PartitionStructureTreeViewNode
import com.redocs.archive.ui.view.partitions.PartitionStructureViewModel
import com.redocs.archive.ui.view.partitions.PartitionsStructureTreeView

class StructureFragment() : Fragment() {

    private var tree: PartitionsStructureTreeView? = null
    private val vm by activityViewModels<PartitionStructureViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val repo = PartitionsStructureRepository(
            ArchiveApplication.partitionsStructureDataSource)

        tree = PartitionsStructureTreeView(context as Context, vm, repo).apply {
                addSelectionListener {
                    itemSelected(it)
                }
                nodeActionListener = { id, action ->
                    when(action) {
                        Action.VIEW -> openDocumentList(id)
                    }
                }
            }

        return object:LinearLayoutCompat(context) {
            override fun generateDefaultLayoutParams(): LayoutParams? =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            }.apply {
                setPadding(8)
                addView(tree)
            }
    }

    private fun itemSelected(node: PartitionStructureTreeViewNode?){
        if(node != null)
            EventBus.publish(PartitionNodeSelectedEvent(node.id))
    }
}

private fun openDocumentList(id: Long) {
    EventBus.publish(PartitionNodeSelectedEvent(id))
    EventBus.publish(ActivateDocumentListEvent())
}