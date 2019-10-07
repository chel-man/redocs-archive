package com.redocs.archive.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redocs.archive.ArchiveApplication
import com.redocs.archive.data.dictionary.DictionaryRepository
import com.redocs.archive.data.files.Repository
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.ui.events.DocumentSelectedEvent
import com.redocs.archive.ui.utils.ActivablePanel
import com.redocs.archive.ui.view.documents.*

class DocumentDetaileFragment() : Fragment(), EventBusSubscriber,
    ActivablePanel {

    override var isActive = false

    private lateinit var documentDetaileView: DocumentDetaileView
    private var firstActivate = true
    private val vm by activityViewModels<DocumentViewModel>()
    private val filesRepo = Repository(ArchiveApplication.filesDataSource)
    private val docsRepo = com.redocs.archive.data.documents.Repository(
        ArchiveApplication.documentsDataSource
    )
    private val dictsRepo = DictionaryRepository(
        ArchiveApplication.dictinaryDataSource
    )

    init {
        subscribe(DocumentSelectedEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when (evt) {
            is DocumentSelectedEvent -> vm.documentId = evt.data
        }
    }

    override fun activate() {

        var doc = vm.document.value as DocumentModel?
        if (firstActivate) {
            if (vm.documentId != Long.MIN_VALUE) {
                if (doc?.id != vm.documentId)
                    startLoadDocument()
                else
                    createView(getController(), doc)
                startObservingModel()
            }
        } else {
            when {
                vm.documentId == Long.MIN_VALUE ->
                    (view as ViewGroup).removeViewAt(0)

                doc?.id != vm.documentId ->
                    startLoadDocument()
                else ->
                    createView(getController(), doc)
            }
        }
        firstActivate = false
    }

    override fun deactivate() {}

    private fun getController(): Controller {
        val ctr = vm.controller ?: Controller(
            vm.coroScope,
            vm.document as MutableLiveData<DocumentModel>,
            docsRepo,
            filesRepo,
            dictsRepo
        )
        vm.controller = ctr
        return ctr as Controller
    }

    private fun startLoadDocument() {
        vm.document.value = DocumentModel(-vm.documentId, "")
        getController().load()
    }

    private fun startObservingModel() {
        vm.document.observe(this, androidx.lifecycle.Observer {
            Log.d("#DF", "model changed")
            it as DocumentModel
            it as DocumentModel
            if (it.isStub)
                createView(getController(), it)
            else
                documentDetaileView.update(it)
        })
    }

    private fun createView(controller: Controller, dm: DocumentModel) {

        with(view as ViewGroup) {
            try {
                removeView(documentDetaileView)
            } catch (npe: NullPointerException) {

            } catch (upa: UninitializedPropertyAccessException) {
            }

            documentDetaileView = DocumentDetaileView(context, controller, dm)

            addView(documentDetaileView)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        ScrollView(context).apply {
            layoutParams = LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(15)
            }
        }
}

class DocumentViewModel : ViewModel() {

    var documentId: Long = Long.MIN_VALUE
    val coroScope= viewModelScope
    var document = MutableLiveData<DocumentModelInterface?>()
    var controller: DocumentControllerInterface? = null
    var topField = 0
}