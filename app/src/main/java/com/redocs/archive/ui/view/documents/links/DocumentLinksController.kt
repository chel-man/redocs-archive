package com.redocs.archive.ui.view.documents.links

import androidx.lifecycle.MutableLiveData
import com.redocs.archive.data.documents.Repository
import com.redocs.archive.data.links.DocumentLinksRepository
import com.redocs.archive.ui.view.documents.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

interface DocumentLinksControllerInterface
class DocumentLinksController(
    private val scope: CoroutineScope,
    private val mutableLiveData: MutableLiveData<DocumentLinksModel>,
    private val docsRepo: Repository,
    private val linksRepo: DocumentLinksRepository
) : DocumentLinksControllerInterface {

    fun load() {
        val od = mutableLiveData.value
        if(od != null) {
            val id = abs(od.documentId)
            scope.launch {
                val docs = docsRepo.get(
                    linksRepo.list(id).map {
                        it.id
                    }
                ).map { it.toModel() }
                mutableLiveData.value = DocumentLinksModel(id, docs)
            }
        }
    }
}
