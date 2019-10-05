package com.redocs.archive.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.data.documents.Repository
import com.redocs.archive.domain.document.Document
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.ui.events.PartitionNodeSelectedEvent
import com.redocs.archive.ui.events.ShowDocumentListRequestEvent
import com.redocs.archive.ui.utils.ActivablePanel
import com.redocs.archive.ui.view.documents.DocumentListView
import com.redocs.archive.ui.view.list.ListViewModel

class DocumentListFragment() : Fragment(), ActivablePanel, EventBusSubscriber {

    override var isActive = false

    private var parentId = Long.MIN_VALUE
    private var documentList: Collection<Document>? = null
    private var documentListChanged = true

    private var listView: DocumentListView? = null
    private val vm by activityViewModels<DocumentListViewModel>()

    /*constructor():super(){
        Log.d("#DLF","RESTORED")
    }*/

    /*constructor(ds: DataSource):this() {
        repo = Repository(ds)
        //Log.d("#DLF","CREATED")
    }*/

    init {
        subscribe(
            PartitionNodeSelectedEvent::class.java,
            ShowDocumentListRequestEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {

        when(evt){
            is PartitionNodeSelectedEvent -> {
                    documentList = null
                    documentListChanged = true
                    parentId = evt.data
                }
            is ShowDocumentListRequestEvent -> {
                    parentId = Long.MIN_VALUE
                    documentList = evt.data
                    documentListChanged = true
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val repo = Repository(
            ArchiveApplication.documentsDataSource)
        listView = DocumentListView(context as Context, vm, repo)
        return listView
    }

    override fun activate() {
        val l = documentList
        if (l != null) {
            if (documentListChanged) {
                documentListChanged = false
                listView?.refresh(l)
            }
        } else if (parentId != Long.MIN_VALUE) {
            listView?.refresh(parentId)
        }
        vm.parentId = parentId
        vm.documentListChanged = documentListChanged
    }

    override fun deactivate() {
        listView?.deactivate()
    }

}

class DocumentListViewModel : ListViewModel() {
    var parentId = Long.MIN_VALUE
    var documentListChanged = true
}

