package com.redocs.archive.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.pm.PackageManager
import com.redocs.archive.BuildConfig
import androidx.core.content.FileProvider
import android.content.Intent
import com.redocs.archive.ui.utils.showError


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

        return Button(context).apply {
            text = "open"
            setOnClickListener {
                val fn = "test.png"
                val dir = File(context.filesDir,"temp")
                dir.mkdirs()
                Log.d("#FileProvider",dir.absolutePath)
                var f = File(dir,fn)
                if(f.exists())
                    f.delete()
                Log.d("#FileProvider", "creating file ...")
                try {

                    context.resources.assets
                        .open(fn).use {
                            val fis = it
                            FileOutputStream(f.absolutePath).use {
                                val ba = ByteArray(512)
                                var bytes = fis.read(ba)
                                while (bytes > 0) {
                                    it.write(ba, 0, bytes)
                                    bytes = fis.read(ba)
                                }
                            }
                        }
                    //f = File(dir,fn)
                }
                catch (e: IOException) {
                    Log.e("#FileProvider", "Exception copying from assets", e);
                    return@setOnClickListener
                }
                Log.e("#FileProvider", "OK ${f.absolutePath} size = ${f.length()}");
                val intent = Intent(Intent.ACTION_VIEW)

// set flag to give temporary permission to external app to use your FileProvider
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

// generate URI, I defined authority as the application ID in the Manifest, the last param is file I want to open
                val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, f)
                Log.d("#FileProvider","uri: $uri")

// I am opening a PDF file so I give it a valid MIME type
                intent.setData/*AndType*/(uri)//, "application/png")

// validate that the device can open your File!
                val pm = activity!!.packageManager
                if (intent.resolveActivity(pm) != null) {
                    startActivity(intent)
                }
                else
                    showError(context,"Cannot find default app")
            }
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
