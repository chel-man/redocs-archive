package com.redocs.archive.ui.view.documents.list

import android.content.Context
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.redocs.archive.R
import com.redocs.archive.data.documents.Repository
import com.redocs.archive.domain.document.Document
import com.redocs.archive.framework.EventBus
import com.redocs.archive.ui.utils.ContextActionSource
import com.redocs.archive.ui.DocumentListViewModel
import com.redocs.archive.ui.events.ContextActionRequestEvent
import com.redocs.archive.ui.events.DocumentSelectedEvent
import com.redocs.archive.ui.events.ShowDocumentEvent
import com.redocs.archive.ui.utils.LongDate
import com.redocs.archive.ui.utils.convertDpToPixel
import com.redocs.archive.ui.utils.showError
import com.redocs.archive.ui.view.list.ListRow
import com.redocs.archive.ui.view.list.ListView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class DocumentListView(
    context: Context,
    vm: DocumentListViewModel,
    repo: Repository
) : ListView<ListRow>(
    context, vm,
    ListAdapter(context,vm.coroScope,repo)
), ContextActionSource {

    override val lockContent = false

    init {
        //Log.d("#DLF","initialising ...")
        longClickListener = {
            EventBus.publish(ContextActionRequestEvent(this))
            true
        }
        selectionListener = {item, selected ->
            if(selected) {
                var doc = null
                if(item != null)
                    vm.coroScope.launch {
                        EventBus.publish(DocumentSelectedEvent(item.id))
                    }
                else
                    EventBus.publish(DocumentSelectedEvent())
            }
        }

        dataSource = DataSource(context, repo).apply {
            parentId = vm.parentId
        }

        //Log.d("#DLF","INITIALISED")

    }

    fun refresh(docs: Collection<Document>) {
        val data = (adapter as ListAdapter).toListRows(docs)


        with(dataSource as DataSource) {
            clear()
            parentId = Long.MIN_VALUE
            this.data += data
        }
        refresh()
        EventBus.publish(DocumentSelectedEvent())
    }

    fun refresh(id: Long) {

        with(dataSource as DataSource) {
            if (parentId != id) {
                clear()
                parentId = id
                refresh()
                EventBus.publish(DocumentSelectedEvent())
            }
            else
                reload()
        }
    }

    fun deactivate(){
        saveState()
    }

    override fun createContextActionMenu(mode: ActionMode, inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.documents_context_menu, menu)
        isContextAction = true
    }

    override fun onDestroyContextAction() {
        isContextAction = false
    }

    override fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.ps_context_add -> Log.d("#DLIST", selectedIds.joinToString(","))
        }
        mode.finish()
        return true
    }

    private class ListAdapter(
        context: Context,
        val scope: CoroutineScope,
        val repo: Repository
    ) : ListView.ListAdapter<ListRow>(
        context
    ) {

        override val columnCount = 4
        override val columnNames = arrayOf("ID", "Name","Created","Updated")

        init {
            require(columnCount == columnNames.size)
            controlClickListener = { item, src ->
                scope.launch {
                    EventBus.publish(
                        DocumentSelectedEvent(item.id)
                    )
                    EventBus.publish(ShowDocumentEvent())
                }
            }

        }

        fun toListRows(docs: Collection<Document>) = docs.map { toListRow(it) }

        private fun toListRow(doc: Document) = DocumentListRow(doc)

        override fun getValueAt(item: ListRow, column: Int): String {
            item as DocumentListRow
            return when (column) {
                0 -> "${item.id}"
                1 -> item.name
                2 -> LongDate.format(context, item.created)
                3 -> LongDate.format(context, item.updated)
                else -> "Error"
            }
        }

        override fun createRowView(context: Context): ListRowView {
            return DocumentRowView(context, columnCount, columnNames, textSize)
        }

        private class DocumentRowView(
            context: Context, count: Int, names: Array<String>, size: Float
        ) : ListRowView(context, count, names, size) {

            override fun renderControls(context: Context): Array<View> {
                return arrayOf(
                    ImageView(context).apply {
                        setImageDrawable(
                            AppCompatResources.getDrawable(
                                context,
                                R.drawable.ic_view_white_24dp
                            )?.apply {
                                DrawableCompat.setTint(
                                    this,
                                    ContextCompat.getColor(
                                        context, R.color.colorPrimary
                                    )
                                )
                            }
                        )
                        val p = convertDpToPixel(12, context)
                        setPadding(p, paddingTop, p, paddingBottom)
                        //setBackgroundColor(ContextCompat.getColor(context,R.color.colorPrimary))
                        ViewCompat.setTooltipText(this, resources.getString(R.string.action_view))
                    }
                )
            }
        }
    }

    private class DocumentListRow(
        doc: Document
    ) : ListRow {
        override val id: Long = doc.id
        override val name: String = doc.name
        val created: Date = doc.created
        val updated: Date = doc.updated
    }

    private class DataSource(
        private val context: Context,
        private val repo: Repository

    ) : ListView.ListDataSource<ListRow>() {

        var data: List<ListRow> = mutableListOf()
        var parentId: Long = Long.MIN_VALUE

        init {

            for (i in 1..200L)
                data += ListView.ListRowBase(
                    i - 1,
                    if ((i % 2L) == 0L)
                        "Item Item Item Item Item Item Item Item Item Item Item Item Item ${i - 1}"
                    else
                        "Item ${i - 1}"
                )
        }

        fun clear() {
            (data as MutableList<ListRow>).clear()
        }

        override fun onError(exception: Exception) {
            Log.e("#DataSource", "${exception.localizedMessage}")
            showError(context, exception)
        }

        override suspend fun loadData(start: Int, size: Int): List<ListRow> {

            if (parentId != Long.MIN_VALUE)
                return repo.list(parentId, start, size).map { DocumentListRow(it) }

            var end = start + size
            if (start > data.size - 1)
                return listOf()
            if (end > data.size - 1)
                end = data.size
            //Log.d("#ListRepo", "RESP: $start : $end")
            delay(500)
            return data.subList(start, end)
        }

        override fun toString(): String {
            return "DS parentId: $parentId"
        }
    }
}
