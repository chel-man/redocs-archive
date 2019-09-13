package com.redocs.archive.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
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
import com.redocs.archive.ui.models.DocumentsViewModel
import com.redocs.archive.ui.utils.convertDpToPixel
import com.redocs.archive.ui.utils.showError
import com.redocs.archive.ui.view.ActivablePanel
import com.redocs.archive.ui.view.list.ListView
import com.redocs.archive.ui.view.list.ListRow

class DocumentsFragment() : Fragment(), ContextActionBridge, ActivablePanel {

    override lateinit var contextActionModeController: ContextActionModeController
    override var isActive = false
    //override var actionListener: (Boolean) -> Unit = {}

    private lateinit var listView: DocumentListView
    private var repo: Repository? = null
    private val vm by activityViewModels<DocumentsViewModel>()

    constructor(ds: DataSource):this() {
        this.repo = Repository(ds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        listView = DocumentListView(context as Context, vm, contextActionModeController).apply {
            setAdapter(ListAdapter(context))
            setDataSource(DocumentListDataSource(context))
            Handler().post {
                refresh()
            }
        }
        return listView
    }

    override fun activate() {
        /*if(!isActive) {
            isActive = true
            with(listView) {
                setDataSource(DocumentListDataSource())
                refresh()
            }
        }*/
    }

    override fun deactivate() {

    }

    private class ListAdapter(context: Context) : ListView.ListAdapter<ListRow>(context) {

        override val columnCount = 2
        override val columnNames = arrayOf("ID","Name")

        init {
            controlClickListener = { item, src ->
                Log.d("#DLA", "Control clicked: ${item.id}")
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

private class DocumentListView(
    context: Context,
    vm: DocumentsViewModel,
    contextActionModeController: ContextActionModeController
) : ListView<ListRow>(context, vm), ContextActionSource {

    override val lockContent = false

    init {
        longClickListener = {
            contextActionModeController.startActionMode(this)
            true
        }
    }

    override fun createContextActionMenu(inflater: MenuInflater, menu: Menu) {
        inflater.inflate(R.menu.documents_context_menu,menu)
        contextAction = true
    }

    override fun onDestroyContextAction() {
        contextAction = false
    }

    override fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.ps_context_add -> Log.d("#DLIST","${selectedItems.joinToString("\n")}")
        }
        mode.finish()
        return true
    }

}

private class DocumentListDataSource(private val context: Context) : ListView.ListDataSource<ListRow>() {

    private lateinit var data:  List<ListRow>

    init {
        val l = mutableListOf<ListRow>()
        for(i in 1..200)
            l.add(ListView.ListRowBase(i-1, "Item Item Item Item Item Item Item Item Item Item Item Item Item ${i-1}"))
        data = l
    }

    override fun onError(exception: Exception) {
        Log.e("#DataSource","${exception.localizedMessage}")
        showError(context, exception)
    }

    override suspend fun loadData(start: Int, size: Int): List<ListRow> {
        var end = start+size
        if(start>data.size-1)
            return listOf()
        if(end>data.size-1)
            end = data.size
        //Log.d("#ListRepo","RESP: $start : $end")
        //delay(2000)
        return data.subList(start,end)
    }
}

