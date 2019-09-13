package com.redocs.archive.ui.view.list

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.PositionalDataSource
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.redocs.archive.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

open class ListView<T: ListRow>(
    context: Context,
    private val vm: ListViewModel
) : RecyclerView(context){

    val selectedItems: List<T>
        get() = (adapter as ListAdapter<T>).selectedItems

    var selectionMode: SelectionMode = SelectionMode.Single
        set(value) {
            vm.selectionMode = value
            (adapter as ListAdapter<T>).selectionMode = value }

    var contextAction: Boolean = false
        set(value) { (adapter as ListAdapter<T>).isContextAction = value }

    var longClickListener: ((item: T)->Boolean)? = null
    var selectionListener: ((item: T?)->Unit)? = null

    private var controller: ListController<T>
    private lateinit var ds: ListDataSource<T>
    private lateinit var list: PagedList<T>
    /*var selected: T? = null
            get() = (adapter as ListAdapter<*>).selected as? T*/

    private var screenRows: Int = 0
    private var scope: CoroutineScope

    init {
        layoutManager = LinearLayoutManager(context)
        setListAdapter(DefaultListAdapter(context) as ListAdapter<T>)

        scope = vm.coroScope
        controller = vm.controller as ListController<T>? ?: createController(scope)

        val tvo = this.viewTreeObserver
        if (tvo.isAlive) {
            tvo.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val tvo = this@ListView.viewTreeObserver
                    if (tvo.isAlive)
                        tvo.removeOnGlobalLayoutListener(this)
                    screenRows = this@ListView.height /
                            com.redocs.archive.ui.utils.spToPx(
                                (adapter as ListAdapter<T>).textSize,
                                context)
                }
            })
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        vm.firstRowPosition = (layoutManager as LinearLayoutManager)
            .findFirstCompletelyVisibleItemPosition()
    }

    protected fun createController(scope: CoroutineScope): ListController<T> {
        return EmptyListController(scope) as ListController<T>
    }

    fun setDataSource(ds: ListDataSource<T>){
        ds.scope = scope
        this.ds = ds
    }

    private fun createList(ds: PositionalDataSource<T>): PagedList<T> {
        val pageSize = screenRows*2
        val prefetch = screenRows
        val maxSize = 2*prefetch+pageSize
        return PagedList(
            ds,
            PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(pageSize)
                .setMaxSize(maxSize)
                .setInitialLoadSizeHint(screenRows*2)
                .setPrefetchDistance(prefetch)
                .build(),
            Executor {
                it.run()
            },
            Executor {
                it.run()
            })
    }

    private fun startObserveData(list: PagedList<T>){
        val ld =MutableLiveData<PagedList<T>>()
        ld.observe(context as LifecycleOwner, Observer {
            (adapter as ListAdapter<T>).submitList(it)
        })
        ld.value = list

    }

    fun refresh(){

        with(adapter as ListAdapter<T>) {
            selectedPosition = vm.selectedRowPosition
            selectionMode = vm.selectionMode
        }

        startObserveData(createList(ds))

        Handler().post{
            if(vm.firstRowPosition > -1)
                scrollToPosition(vm.firstRowPosition)
        }

    }

    protected abstract class ListController<T : ListRow>(
        private val scope: CoroutineScope
    ) : ListControllerInterface {

    }

    private class EmptyListController(scope: CoroutineScope) : ListController<ListRow>(scope)

    private fun onLongClick(item: T): Boolean{
        val l = longClickListener
        if(l != null)
            return l(item)
        else
            return false
    }

    private fun onItemSelected(item: T?){
        vm.selectedRowPosition = (adapter as ListAdapter<T>).selectedPosition
        val l = selectionListener
        if(l != null)
            l(item)
    }

    fun setAdapter(adapter: ListAdapter<T>) {
        setListAdapter(adapter)
    }

    private fun setListAdapter(adapter: ListAdapter<T>) {
        this.adapter = adapter
        with(adapter) {
            selectedPosition = vm.selectedRowPosition
            selectionListener = this@ListView::onItemSelected
            longClickListener = this@ListView::onLongClick
        }

    }

    /*class DataModel<T: ListRow>(
        var selectedPositions: Array<Int> = arrayOf(),
        val firstItemPosition: Int = 0,
        val data: List<T> = listOf<T>(),
        val operation: Operation? = null
    ) : ListDataModel
    {
        override fun toString() = "$operation $data"
    }*/

    class ListRowBase(
        override val id: Int,
        override val name: String) : ListRow {

        override fun toString(): String {
            return "$id : $name"
        }
    }

    private class DefaultListAdapter(context: Context) : ListAdapter<ListRow>(context) {
        override val columnCount = 0
        override val columnNames: Array<String> = emptyArray()

        override fun getValueAt(item: ListRow, column: Int): String {
            return ""
        }
    }

    abstract class ListAdapter<T: ListRow>(
        private val context: Context
    ) : PagedListAdapter<T, RecyclerView.ViewHolder>(
        object: DiffUtil.ItemCallback<T>(){
            override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
                return false
            }

            override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
                return false
            }}
    ) {


        var selectionListener: ((item: T?) -> Unit)? = null
        var longClickListener: ((item: T) -> Boolean)? = null
        var controlClickListener: ((item: T, button: View) -> Unit)? = null
        var selectedPosition = -1
        var textSize = 18.0F
        var selectionMode: SelectionMode = SelectionMode.Single
            set(value){
                isShowCheckBoxes = value != SelectionMode.Single
                field = value
            }

        val selectedItems: List<T>
            get() {
                Log.d("#ADAPTER","selected: ${selectedPositions.joinToString (",")}")
                val items = mutableListOf<T>()
                for (pos in selectedPositions)
                    items += getItem(pos) as T
                if(selectedPosition >-1 && !selectedPositions.contains(selectedPosition))
                    items += getItem(selectedPosition) as T

                return items
            }

        var isContextAction: Boolean = false
            set(value) {
                isShowCheckBoxes = value || selectionMode == SelectionMode.Multiply
                if(!value) {
                    selectedPositions.clear()
                    if(selectedPosition > -1)
                        prevSelected?.isActivated = true
                }
                field = value
            }

        protected abstract val columnCount: Int
        protected abstract val columnNames: Array<String>
        protected abstract fun getValueAt(item: T, column: Int): String

        private val selectedPositions = mutableSetOf<Int>()
        private val createdViews = mutableSetOf<ListRowView>()
        private var prevSelected: ListRowView? = null
        private var isShowCheckBoxes: Boolean = false
            set(value){
                if(value != field) {
                    Log.d("#AD","Showing checkboxes: $value cnt:${createdViews.size}")
                    for (v in createdViews) {
                        with(v.checkBox) {
                            visibility = if (value) View.VISIBLE else View.GONE
                            if (!isVisible)
                                isChecked = false
                        }
                        if(selectionMode == SelectionMode.Single) {
                            for (cv in v.controlViews)
                                cv.visibility = if (!value) View.VISIBLE else View.GONE
                        }
                    }
                    Log.d("#AD","Showing checkboxes: END")
                    field = value
                }
            }


        open protected fun createRowView(context: Context): ListRowView {
            return ListRowView(context, columnCount, columnNames,textSize)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            val v = createRowView(context)
            createdViews += v
            return ListViewHolder(v)
        }

        /*override fun onViewRecycled(holder: ViewHolder) {
            super.onViewRecycled(holder)
            createdViews -= (holder.itemView as ListRowView)
        }*/

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position) as T

            //Log.d("#View","${item.id}  /  $position  ${if(item.id != position) "!!!" else ""}")
            //if(item !=null) {
            (holder.itemView as ListRowView).apply {
                var i = 0
                for(tv in columnViews)
                    tv?.text = getValueAt(item,i++)

                if(selectionMode == SelectionMode.Single && !isContextAction){
                    if (selectedPosition == position) {
                        isActivated = true
                        prevSelected = this
                    }
                    else
                        isActivated = false
                }
                else {
                    isActivated = selectedPositions.contains(position)
                    /*if(isActivated)
                        Log.d("#VIEW","Selected $position")*/
                }

                val view = this

                with(checkBox){
                    visibility = if(isShowCheckBoxes) View.VISIBLE else GONE
                    setOnCheckedChangeListener {buttonView, isChecked ->  }
                    isChecked = selectedPositions.contains(position)
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        if(isChecked) {
                            selectedPositions += position
                            Log.d("#VIEW","SELECTED ${selectedPositions.joinToString ("\n")}")
                        }
                        else {
                            selectedPositions -= position
                            Log.d("#VIEW", "UNSELECTED ${selectedPositions.joinToString("\n")}")
                        }
                        view.isActivated = isChecked
                    }
                }

                setClickListener {
                    if(!isContextAction)
                        selectItem(item, this, position)
                }

                setLongClickListener {
                    if (isContextAction)
                        false
                    else{
                        //if (selectedPosition != position)
                            selectItem(item, this, position)
                        val l = longClickListener
                        if (l != null)
                            l(item)
                        else
                            false
                    }
                }

                for(cv in controlViews){
                    cv.visibility = if(selectionMode == SelectionMode.Single && isContextAction) GONE else View.VISIBLE
                    cv.setOnClickListener {
                        if(!isContextAction)
                            controlClickListener?.invoke(item,cv)
                    }
                }
            }
            //}
        }

        private fun selectItem(item: T, itemView: ListRowView, pos: Int) {

            if (selectionMode == SelectionMode.Single && !isContextAction){
                prevSelected?.isActivated = false
                prevSelected?.checkBox?.isChecked = false
                selectedPosition = pos
                prevSelected = itemView
            }
            itemView.isActivated = true
            itemView.checkBox.isChecked = true
            selectionListener?.invoke(if (pos > -1) item else null)
        }



        private class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

        open protected class ListRowView(context: Context, count: Int,
                                  private val columnNames: Array<String>,
                                  textSize: Float
        ) : CardView(context) {

            var checkBox: CheckBox
            var columnViews = arrayOfNulls<TextView>(count)
            var controlViews = emptyArray<View>()

            private var labels = emptyArray<Label?>()
            private var fieldsLayout: LinearLayoutCompat

            private object Conf {
                val textTypeFace = Typeface.NORMAL
                var backgroundColor = Color.TRANSPARENT
                @ColorRes
                var textColor = R.color.colorPrimaryDark
                @ColorRes
                var selectedBgColor = R.color.colorPrimary
                val selectedTextColor = Color.WHITE
                val selectedTextTypeface = Typeface.BOLD
            }

            init{

                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(5)
                }

                val hl = LinearLayoutCompat(context)
                fieldsLayout = LinearLayoutCompat(context).apply {
                    layoutParams = LinearLayoutCompat.LayoutParams(
                        0,
                        LayoutParams.WRAP_CONTENT
                    ).apply {
                        weight = 0.8F
                    }
                }

                val (labels,columnViews) = renderFieldsLayout(
                    context,textSize,fieldsLayout,count,columnNames)

                this.labels = labels
                this.columnViews = columnViews

                hl.addView(fieldsLayout)

                val controls = renderControls(context)
                controlViews = controls
                for(v in controls) {
                    v.layoutParams = LinearLayoutCompat.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT)

                    hl.addView(v)
                }
                checkBox = CheckBox(context).apply {
                    layoutParams = LinearLayoutCompat.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT)
                    visibility = View.GONE
                }
                hl.addView(checkBox)

                addView(hl)

                if (Conf.selectedBgColor > 0) {
                    Conf.selectedBgColor = ContextCompat.getColor(context, Conf.selectedBgColor)
                    Conf.textColor = ContextCompat.getColor(context, Conf.textColor)
                }

            }

            open protected fun renderControls(context: Context): Array<View> {
                return emptyArray()
            }

            open protected fun renderFieldsLayout(
                context: Context,
                textSize: Float,
                fieldsLayout: LinearLayoutCompat,
                count: Int,
                columnNames: Array<String>
            ): Pair<Array<Label?>, Array<TextView?>> {

                var columnViews: Array<TextView?> = arrayOfNulls(count)
                var labels = emptyArray<Label?>()
                var i = 0
                if(columnNames.isEmpty()) {
                    while (count - i > 0) {
                        val tv = TextView(context).apply {
                            this.textSize = textSize
                        }
                        fieldsLayout.addView(tv)
                        columnViews[i++] = tv
                    }
                }
                else {
                    fieldsLayout.orientation = LinearLayoutCompat.VERTICAL
                    columnViews = arrayOfNulls(columnNames.size)
                    labels = arrayOfNulls<Label?>(columnNames.size)
                    for(cn in columnNames){
                        val hl = LinearLayoutCompat(context)
                        labels[i] = Label(context, "$cn: ").apply {
                            this.textSize = textSize
                            setTypeface(null,Typeface.BOLD)
                        }
                        hl.addView(labels[i])
                        val tv = TextView(context).apply { this.textSize = textSize }
                        hl.addView(tv)
                        fieldsLayout.addView(hl)
                        columnViews[i++] = tv
                    }
                }

                return labels to columnViews
            }

            fun setClickListener(l: ()->Unit) {
                fieldsLayout.setOnClickListener {
                    l()
                }
            }

            fun setLongClickListener(l: () -> Boolean) {
                fieldsLayout.setOnLongClickListener {
                    l()
                }
            }

            override fun setActivated(activated: Boolean) {
                super.setActivated(activated)

                var bgc = Conf.backgroundColor
                var fc = Conf.textColor
                var tf = Conf.textTypeFace

                if (activated) {
                    bgc = Conf.selectedBgColor
                    fc = Conf.selectedTextColor
                    tf = Conf.selectedTextTypeface
                }

                fieldsLayout.setBackgroundColor(bgc)

                for(i in 0..columnViews.size-1) {
                    columnViews[i]?.apply {
                        //setBackgroundColor(bgc)
                        setTextColor(fc)
                        setTypeface(null, tf)
                    }
                    labels[i]?.setTextColor(fc)
                }
                //Log.d("#NodeView","${textView.text} active: $activated")
            }

        }

        class Label(context: Context, text: String) : TextView(context){
            init {
                this.text=text
            }
        }
    }

    abstract class ListDataSource<T: ListRow> : PositionalDataSource<T>() {

        lateinit var scope: CoroutineScope

        abstract fun onError(exception: Exception)

        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<T>
        ) {
            fetchData(0,params.requestedLoadSize, initCallback = callback)
            /*scope.launch(Dispatchers.IO) {
                try {
                    Log.d("#DataSource","IL: ${params.requestedLoadSize}")
                    val l: List<T> = loadData(0,params.requestedLoadSize)
                    callback.onResult(l, 0)
                }catch (ex: Exception){
                    Log.e("#DataSource IL","${ex.localizedMessage}")
                    callback.onResult(emptyList(),0)
                }
            }*/

        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
            fetchData(params.startPosition,params.loadSize, callback = callback)
            /*scope.launch(Dispatchers.IO) {
                var l: List<T> = emptyList()
                try {
                    Log.d("#DataSource LR","${params.startPosition} : ${params.loadSize}")
                    l = loadData(params.startPosition, params.loadSize)
                }catch (ex: Exception){
                    Log.e("#DataSource LR","${ex.localizedMessage}")
                    //callback.onResult(emptyList())
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(l)
                }
            }*/
        }

        private fun fetchData(start: Int, size: Int,
                              initCallback: LoadInitialCallback<T>? = null,
                              callback: LoadRangeCallback<T>? = null) {

            scope.launch(Dispatchers.IO) {
                var l: List<T> = emptyList()
                try {
                    //Log.d("#DataSource","$start : $size")
                    l = loadData(start, size)
                }catch (ex: Exception){
                    onError(ex)
                }
                withContext(Dispatchers.Main) {
                    initCallback?.onResult(l,0)
                    callback?.onResult(l)
                }
            }

        }

        protected abstract suspend fun loadData(startPosition: Int, loadSize: Int): List<T>
    }

    enum class SelectionMode {
        Single,
        Multiply
    }
}

interface ListRepository<Value: ListRow> {

    suspend fun load(id: Int, size: Int): List<Value>
}

open class ListViewModel : ViewModel() {
    var selectionMode = ListView.SelectionMode.Single
    val coroScope
        get() =  viewModelScope

    var controller: ListControllerInterface? = null
    var firstRowPosition = -1
    var selectedRowPosition = -1
    //val data: MutableLiveData<ListDataModel>
}

//interface ListDataModel
interface ListControllerInterface
interface ListRow {
    val id: Int
    val name: String
}

interface Clickable {
    fun clickListener(l:(rsc:View)->Unit)
}

class ClickableImageView(context: Context) : ImageView(context), Clickable {
    override fun clickListener(l: (rsc: View) -> Unit) {
        setOnClickListener {
            l(it)
        }
    }
}