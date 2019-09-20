package com.redocs.archive.ui.models

import com.redocs.archive.data.documents.Repository
import com.redocs.archive.ui.view.list.ListViewModel

class DocumentsViewModel : ListViewModel() {
    var repository: Repository? = null
}