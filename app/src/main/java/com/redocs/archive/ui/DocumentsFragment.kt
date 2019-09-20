package com.redocs.archive.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redocs.archive.R
import com.redocs.archive.data.documents.DataSource
import com.redocs.archive.data.documents.Repository
import com.redocs.archive.domain.document.Document
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.framework.unsubscribe
import com.redocs.archive.ui.events.PartitionNodeSelectedEvent
import com.redocs.archive.ui.events.ShowDocumentEvent
import com.redocs.archive.ui.events.ShowDocumentListRequestEvent
import com.redocs.archive.ui.models.DocumentsViewModel
import com.redocs.archive.ui.utils.convertDpToPixel
import com.redocs.archive.ui.utils.showError
import com.redocs.archive.ui.view.ActivablePanel
import com.redocs.archive.ui.view.list.ListRow
import com.redocs.archive.ui.view.list.ListView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DocumentsFragment() : Fragment(), ContextActionBridge, ActivablePanel, EventBusSubscriber {

    private var unsubscribeListener: () -> Unit
    private var documentList: List<Document>? = null
    private var documentListChanged = true

    override var isActive = false

    private var parentId = Long.MIN_VALUE

    override var contextActionModeController: ContextActionModeController = ContextActionModeControllerStub()
        set(value){
            listView?.contextActionModeController = value
            field = value
        }

    private var listView: DocumentListView? = null
    private var repo: Repository? = null
    private val vm by activityViewModels<DocumentsViewModel>()

    constructor(ds: DataSource):this() {
        repo = Repository(ds)
    }

    init {
        unsubscribeListener = subscribe(
            PartitionNodeSelectedEvent::class.java,
            ShowDocumentListRequestEvent::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribeListener()
    }

    override suspend fun onEvent(evt: EventBus.Event<*>) {

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
        val repo = repo ?: vm.repository as Repository
        vm.repository=repo
        listView = DocumentListView(context as Context, vm).apply {
            contextActionModeController = this@DocumentsFragment.contextActionModeController
            //selectionMode = ListView.SelectionMode.Multiply
        }
        return listView
    }

    override fun activate() {
        val l = documentList
        if(l != null) {
            if(documentListChanged) {
                documentListChanged = false
                listView?.refresh(l.map { it.toListRow() })
            }
        }
        else if(parentId != Long.MIN_VALUE)
            listView?.refresh(parentId)
    }

    override fun deactivate() {
    }

}

private class DocumentListView(
    context: Context,
    vm: DocumentsViewModel
) : ListView<ListRow>(
        context, vm,
        ListAdapter(context,vm.coroScope,vm.repository as Repository)
    ), ContextActionSource
{

    var contextActionModeController: ContextActionModeController? = null

    override val lockContent = false

    init {
        longClickListener = {
            contextActionModeController?.startActionMode(this)
            true
        }
        dataSource = DocumentListDataSource(context, vm.repository as Repository)

    }

    fun refresh(data: Collection<ListRow>) {

        with(dataSource as DocumentListDataSource){
            clear()
            parentId = Long.MIN_VALUE
            this.data += data
        }
        refresh()
    }

    fun refresh(id: Long){

        val ds = dataSource as DocumentListDataSource
        if(ds.parentId != id) {
            with(ds){
                clear()
                parentId = id
            }
            refresh()
        }
    }

    override fun createContextActionMenu(inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.documents_context_menu,menu)
        isContextAction = true
    }

    override fun onDestroyContextAction() {
        isContextAction = false
    }

    override fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.ps_context_add -> Log.d("#DLIST","${selectedIds.joinToString(",")}")
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

        override val columnCount = 2
        override val columnNames = arrayOf("ID","Name")

        init {
            controlClickListener = { item, src ->
                scope.launch {
                    EventBus.publish(
                        ShowDocumentEvent(
                            repo.get(item.id)))

                }
            }

        }

        override fun getValueAt(item: ListRow, column: Int): String {
            return when(column){
                0 -> "${item.id}"
                1 -> item.name
                else -> "Error"
            }
        }

        override fun createRowView(context: Context): ListRowView {
            return DocumentRowView(context,columnCount,columnNames,textSize)
        }

        private class DocumentRowView(
            context: Context, count: Int, names:Array<String>,size: Float
        ) : ListRowView(context,count,names,size){

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
                                        context,R.color.colorPrimary))
                            }
                        )
                        val p = convertDpToPixel(12,context)
                        setPadding(p,paddingTop,p,paddingBottom)
                        //setBackgroundColor(ContextCompat.getColor(context,R.color.colorPrimary))
                        ViewCompat.setTooltipText(this,resources.getString(R.string.action_view))
                    }
                )
            }
        }
    }
}

private class DocumentListDataSource(
    private val context: Context,
    private val repo: Repository

) : ListView.ListDataSource<ListRow>() {

    var data:  List<ListRow> = mutableListOf()
    var parentId: Long = Long.MIN_VALUE

    init {

        for(i in 1..200L)
            data += ListView.ListRowBase(i-1,
                if( (i % 2L) == 0L)
                    "Item Item Item Item Item Item Item Item Item Item Item Item Item ${i-1}"
                else
                    "Item ${i-1}")
    }

    fun clear(){
        (data as MutableList<ListRow>).clear()
    }

    override fun onError(exception: Exception) {
        Log.e("#DataSource","${exception.localizedMessage}")
        showError(context, exception)
    }

    override suspend fun loadData(start: Int, size: Int): List<ListRow> {

        if(parentId != Long.MIN_VALUE)
            return repo.list(parentId,start,size).map { it.toListRow() }

        var end = start + size
        if (start > data.size - 1)
            return listOf()
        if (end > data.size - 1)
            end = data.size
        //Log.d("#ListRepo", "RESP: $start : $end")
        delay(500)
        return data.subList(start, end)
    }

}

private fun Document.toListRow() = ListView.ListRowBase(id,name) as ListRow

