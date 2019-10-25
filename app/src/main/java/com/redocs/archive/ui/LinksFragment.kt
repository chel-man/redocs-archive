package com.redocs.archive.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import com.redocs.archive.ui.utils.ActivablePanel
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.data.links.DocumentLinksRepository
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.ui.events.DocumentSelectedEvent
import com.redocs.archive.ui.view.documents.links.*


class LinksFragment() : Fragment(), ActivablePanel, EventBusSubscriber {

    override var isActive = false

    private var firstActivate = false
    private var linksRepo = DocumentLinksRepository(
        ArchiveApplication.documentLinksDataSource
    )
    private val docsRepo = com.redocs.archive.data.documents.Repository(
        ArchiveApplication.documentsDataSource
    )
    private val vm by activityViewModels<DocumentLinksViewModel>()
    private lateinit var documentLinksView: DocumentLinksView


    init {
        subscribe(DocumentSelectedEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when (evt) {
            is DocumentSelectedEvent -> vm.documentId = evt.data
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =

        LinearLayoutCompat(context).apply {
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }
        }

    override fun activate() {

        var docs = vm.model.value as DocumentLinksModel?
        if (firstActivate) {
            if (vm.documentId != Long.MIN_VALUE) {
                if (docs?.documentId != vm.documentId)
                    startLoadDocument()
                else
                    createView(getController(), docs)
                startObservingModel()
            }
        } else {
            when {

                vm.documentId == Long.MIN_VALUE -> {
                    try {
                        (view as ViewGroup).removeViewAt(0)
                    }catch (npe: java.lang.NullPointerException){}
                }
                docs?.documentId != vm.documentId ->
                    startLoadDocument()
                else ->
                    createView(getController(), docs)
            }
        }
        firstActivate = false
    }

    override fun deactivate() {}

    private fun getController(): DocumentLinksController {
        val ctr = vm.controller ?: DocumentLinksController(
            vm.coroScope,
            vm.model as MutableLiveData<DocumentLinksModel>,
            docsRepo,
            linksRepo)
        vm.controller = ctr
        return ctr as DocumentLinksController
    }

    private fun startLoadDocument() {
        vm.model.value = DocumentLinksModel(-vm.documentId, emptyList())
        getController().load()
    }

    private fun startObservingModel() {
        with(vm.model) {
            removeObservers(this@LinksFragment)
            observe(this@LinksFragment, androidx.lifecycle.Observer {
                android.util.Log.d("#DLF", "model changed")
                it as DocumentLinksModel
                if (it.isStub)
                    createView(getController(), it)
                else
                    documentLinksView.update(it)
            })
        }
    }

    private fun createView(controller: DocumentLinksController, dm: DocumentLinksModel) {

        with(view as ViewGroup) {
            try {
                removeView(documentLinksView)
            } catch (npe: NullPointerException) {

            } catch (upa: UninitializedPropertyAccessException) {
            }

            documentLinksView = DocumentLinksView(context, controller, dm)

            addView(documentLinksView)
        }
    }


}

class DocumentLinksViewModel : ViewModel() {

    var documentId: Long = Long.MIN_VALUE
    val coroScope= viewModelScope
    var model = MutableLiveData<DocumentLinksModelInterface?>()
    var controller: DocumentLinksControllerInterface? = null
}