package com.redocs.archive.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import com.redocs.archive.data.files.DataSource
import com.redocs.archive.data.files.Repository
import com.redocs.archive.ui.utils.ActivablePanel
import com.redocs.archive.ui.view.list.ListView
import com.redocs.archive.ui.view.list.ListRow
import com.redocs.archive.ui.view.panels.StackPanel
import kotlinx.coroutines.delay

class FilesFragment() : Fragment(), ActivablePanel {

    override var isActive = false

    private lateinit var listView: ListView<ListRow>
    //override var actionListener: (Boolean) -> Unit = {}
    private var repo: Repository? = null
    //private val vm by activityViewModels<DocumentListViewModel>()

    constructor(ds: DataSource):this() {
        this.repo = Repository(ds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return StackPanel(context).apply {
            addPanel("Panel 1",
                LinearLayoutCompat(context).apply {
                    layoutParams = LinearLayoutCompat.LayoutParams(
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT
                    ).apply {
                        setMargins(15)
                    }
                    orientation = LinearLayoutCompat.VERTICAL

                    addView(
                        TextView(context).apply { text = "Text Panel 1" }
                    )
                }
            )
            addPanel("Panel 2", TextView(context).apply { text = "Text Panel 2" })
            addPanel("Panel 3", TextView(context).apply { text = "Text Panel 3" })
        }
    }

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
        for(i in 1..20L)
            l.add(ListView.ListRowBase(i-1, "FileInfo ${i-1}"))
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
