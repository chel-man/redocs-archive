package com.redocs.archive.ui.view.documents.links

import com.redocs.archive.ui.view.documents.DocumentModel

interface DocumentLinksModelInterface

data class DocumentLinksModel(
    val documentId: Long,
    val data: Collection<DocumentModel>
) : DocumentLinksModelInterface {

    var isStub = documentId < 0

}