package com.redocs.archive.ui.view.documents.links

import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import com.redocs.archive.ui.view.list.ListRow
import com.redocs.archive.ui.view.list.ListView

class DocumentLinksView (
    context: Context,
    private val controller: DocumentLinksController,
    private var model: DocumentLinksModel

) : LinearLayoutCompat(context) {

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            setMargins(2)
        }

        orientation = VERTICAL

        addView(
            if(model.isStub) {
                ProgressBar(context).apply {
                    this.isIndeterminate = true
                }
            }
            else
                DocumentList(context)
        )
    }

    fun update(lm: DocumentLinksModel) {

        removeViewAt(0)
        model = lm
        addView(DocumentList(context))

    }

}

class DocumentList(
    context: Context
) : ListView<ListRow>(
    context
){

}
