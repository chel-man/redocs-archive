package com.redocs.archive.ui

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.redocs.archive.data.files.DataSource
import com.redocs.archive.data.files.Repository
import com.redocs.archive.ui.models.DocumentsViewModel
import com.redocs.archive.ui.view.ActivablePanel
import com.redocs.archive.ui.view.list.ListView
import com.redocs.archive.ui.view.list.ListRow
import kotlinx.coroutines.delay

class FilesFragment() : Fragment(), /*ContextActionBridge,*/ ActivablePanel {

    //override lateinit var contextActionModeController: ContextActionModeController
    override var isActive = false

    private lateinit var listView: ListView<ListRow>
    //override var actionListener: (Boolean) -> Unit = {}
    private var repo: Repository? = null
    private val vm by activityViewModels<DocumentsViewModel>()

    constructor(ds: DataSource):this() {
        this.repo = Repository(ds)
    }

    /*override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        listView = ListView<ListRow>(context as Context,vm).apply {
            setAdapter(ListAdapter(context))
            setDataSource(FileListDataSource())
            //refresh()
        }
        return listView
    }*/

    override fun activate() {
        /*if(!isActive) {
            isActive = true
            with(listView) {
                setDataSource(FileListDataSource())
                refresh()
            }
        }*/
    }

    override fun deactivate() {

    }

    private class ListAdapter(context: Context) : ListView.ListAdapter<ListRow>(context) {
        override val columnNames = emptyArray<String>()
        override val columnCount = 2

        override fun getValueAt(item: ListRow, column: Int): String {
            return when(column){
                0 -> "${item.id}"
                1 -> item.name
                else -> "Error"
            }
        }
    }
}

private class FileListDataSource : ListView.ListDataSource<ListRow>() {

    override fun onError(exception: Exception) {

    }

    private lateinit var data:  List<ListRow>

    init {
        val l = mutableListOf<ListRow>()
        for(i in 1..20)
            l.add(ListView.ListRowBase(i-1, "File ${i-1}"))
        data = l
    }

    override suspend fun loadData(start: Int, size: Int): List<ListRow> {
        var end = start+size
        if(start>data.size-1)
            return listOf()
        if(end>data.size-1)
            end = data.size
        delay(1000)
        return data.subList(start,end)
    }
}
