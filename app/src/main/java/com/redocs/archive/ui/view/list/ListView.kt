package com.redocs.archive.ui.view.list

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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
import kotlinx.coroutines.*
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class ListView<T: ListRow>(
    context: Context,
    private val vm: ListViewModel,
    adapter: ListAdapter<T>
) : RecyclerView(context){

    val selectedIds: List<Long>
        get() = (adapter as ListAdapter<T>).selectedIds

    var selectionMode: SelectionMode = SelectionMode.Single
        set(value) {
            vm.selectionMode = value
            (adapter as ListAdapter<T>).selectionMode = value
            field = value}

    var isContextAction: Boolean = false
        set(value) { (adapter as ListAdapter<T>).isContextAction = value }

    var longClickListener: ((item: T)->Boolean)? = null
    var selectionListener: ((item: T?, selected: Boolean)->Unit)? = null

    var dataSource: ListDataSource<T> = EmptyDataSource as ListDataSource<T>
        set(value){
            //Log.d("#LV","DS IS SETTED UP $value")

            value.scope = vm.coroScope
            //this.ds = value
            field = value
        }

    private object EmptyDataSource : ListDataSource<ListRow>() {
        override fun onError(exception: Exception) {}
        override suspend fun loadData(startPosition: Int, loadSize: Int): List<ListRow> = emptyList()

    }

    private var controller: ListController<T>
    //private lateinit var ds: ListDataSource<T>

    private var screenRows: Int = 0

    init {

        //Log.d("#LISTVIEW","init entered")
        layoutManager = LinearLayoutManager(context)
        this.adapter = adapter

        controller = vm.controller as ListController<T>? ?: createController(vm.coroScope)

        /*val tvo = this.viewTreeObserver
        if (tvo.isAlive) {
            //Log.d("#LISTVIEW","init viewTreeObserver alive")
            tvo.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val tvo = this@ListView.viewTreeObserver
                    if (tvo.isAlive)
                        tvo.removeOnGlobalLayoutListener(this)
                    screenRows = this@ListView.height /
                            com.redocs.archive.ui.utils.spToPx(
                                (adapter as ListAdapter<T>).textSize,
                                context)
                    //Log.d("#LISTVIEW","init screen rows: $screenRows")
                    if(vm.liveList?.value != null)
                        initAdapter(adapter,vm)
                }
            })
        }*/

        Handler().post{
            screenRows = this@ListView.height /
                    com.redocs.archive.ui.utils.spToPx(
                        (adapter as ListAdapter<T>).textSize,
                        context)
            //Log.d("#LISTVIEW","init screen rows: $screenRows")
            if(vm.liveList?.value != null)
                initAdapter(adapter,vm)

        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        saveState()
    }

    protected fun saveState(){
        vm.state = layoutManager?.onSaveInstanceState()
    }

    fun selectById(id: Long) =
        vm.coroScope.launch {
            val pos = (adapter as ListAdapter<T>).selectById(id)
            if (pos > -1)
                scrollToPosition(pos)
        }

    protected fun createController(scope: CoroutineScope) =
        EmptyListController(scope) as ListController<T>

    private fun createList(ds: ListDataSource<T>, itemRows: Int): PagedList<T> {
        val itemsOnScreen = screenRows/itemRows
        val pageSize = itemsOnScreen*2
        val prefetch = itemsOnScreen/2
        val maxSize = 3*pageSize

        return PagedList(
            ds,
            PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(pageSize)
                .setMaxSize(maxSize)
                .setInitialLoadSizeHint(itemsOnScreen)
                .setPrefetchDistance(prefetch)
                .build(),
            Executor {
                it.run()
            },
            Executor {
                it.run()
            })
    }

    fun reload(){
        //Log.d("#LV","RELOAD")
        initAdapter(adapter as ListAdapter<T>,vm)
    }

    fun refresh(){
        //Log.d("#LV","REFRESH")
        clearViewModel(vm)
        initAdapter(adapter as ListAdapter<T>,vm)
    }

    private fun clearViewModel(vm: ListViewModel){
        with(vm){
            liveList?.value = null
            selectedIds.clear()
            selectedId = -1
            state = null
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

    private fun onItemSelected(item: T?, selected: Boolean){
        if(item != null) {
            if(selectionMode == SelectionMode.Single && !isContextAction){
                vm.selectedId =item.id
            }
            else {
                if (selected)
                    vm.selectedIds += item.id
                else
                    vm.selectedIds -= item.id
            }
            selectionListener?.invoke(item, selected)
        }
    }

    private fun initAdapter(
        adapter: ListAdapter<T>,
        vm: ListViewModel)
    {

        with(adapter) {
            try{
                ld.value = null
            }catch (ex: Exception){
                Log.e("#LV","${ex.localizedMessage}")
            }
            selectedId = vm.selectedId
            selectedIds += vm.selectedIds
            selectionMode = vm.selectionMode
            selectionListener = this@ListView::onItemSelected
            longClickListener = this@ListView::onLongClick
            val liveList = vm.liveList ?: MutableLiveData()
            vm.liveList = liveList
            var list = liveList.value
            if(list == null)
                list = createList(dataSource,columnCount)

            ld.value = list as PagedList<T>
            liveList.value = list
        }

        if(vm.state != null) {
            layoutManager?.onRestoreInstanceState(vm.state)
            //Log.d("#LV","STATE RESTORED")
        }
        //Log.d("#LV","ADAPTER INITIALIZED")

    }

    class ListRowBase(
        override val id: Long,
        override val name: String
    ) : ListRow {

        override fun toString(): String {
            return "$id : $name"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ListRow

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }


    }

    abstract class ListAdapter<T: ListRow>(
        protected val context: Context
    ) : PagedListAdapter<T, RecyclerView.ViewHolder>(
        object: DiffUtil.ItemCallback<T>(){
            override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
                return oldItem.id == newItem.id
            }}
    ) {

        var selectionListener: ((item: T?, selected: Boolean) -> Unit)? = null
        var longClickListener: ((item: T) -> Boolean)? = null
        var controlClickListener: ((item: T, button: View) -> Unit)? = null
        var selectedId: Long = -1
        var textSize = 18.0F
        var selectionMode: SelectionMode = SelectionMode.Single
            set(value){
                isShowCheckBoxes = value != SelectionMode.Single
                field = value
            }

        var isContextAction: Boolean = false
            set(value) {
                isShowCheckBoxes = value || selectionMode == SelectionMode.Multiply
                if(!value) {
                    selectedIds.clear()
                    if(selectedId > -1)
                        prevSelected?.isActivated = true
                }
                field = value
            }

        val selectedIds = mutableListOf<Long>()
        var ld: MutableLiveData<PagedList<T>> = MutableLiveData()
        abstract val columnCount: Int

        protected abstract val columnNames: Array<String>
        protected abstract fun getValueAt(item: T, column: Int): String

        private val createdViews = mutableSetOf<ListRowView>()
        private var prevSelected: ListRowView? = null
        private var isShowCheckBoxes: Boolean = false
            set(value){
                if(value != field) {
                    ////Log.d("#AD","Showing checkboxes: $value cnt:${createdViews.size}")
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
                    ////Log.d("#AD","Showing checkboxes: END")
                    field = value
                }
            }

        init {
            ld.observe(context as LifecycleOwner, Observer {
                submitList(it)
            })
        }

        suspend fun selectById(id: Long) = withContext(Dispatchers.IO) {

            val list = ld.value as PagedList<T>
            var pos = 0
            var page = 0

            while (true) {
                var count = 0
                var delay = 100L
                var loadingPage = false
                while(true) {
                    if(count>5) {
                        Log.e("#AD","Not found last: $pos retries: $count")
                        return@withContext -1
                    }
                    try {
                        //Log.d("#AD", "=> $pos / $count / ${list.positionOffset}")
                        list.loadAround(pos)
                        if(loadingPage){
                            loadingPage = false
                            pos = 0
                            continue
                        }
                        break
                    }catch (ex: IndexOutOfBoundsException){
                        Log.e("#AD", "======================")
                        loadingPage = true
                        delay(delay)
                        delay *=2
                        pos -= list.positionOffset
                        count++
                    }
                }
                val item = list[pos]
                if(item?.id == id)
                    break
                pos++
            }
            selectedId = id
            withContext(Dispatchers.Main) {
                notifyItemChanged(pos)
            }
            pos
        }

        open protected fun createRowView(context: Context): ListRowView {
            return ListRowView(context, columnCount, columnNames,textSize)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            val v = createRowView(context)
            createdViews += v
            return ListViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position) as T
            val itemId = item.id
            ////Log.d("#AD","${item.id} / $position $selectedId")
            (holder.itemView as ListRowView).apply {
                var i = 0
                for(tv in columnViews)
                    tv?.text = getValueAt(item,i++)

                if(selectionMode == SelectionMode.Single && !isContextAction){
                    if (selectedId == itemId) {
                        isActivated = true
                        prevSelected = this
                    }
                    else
                        isActivated = false
                }
                else
                    isActivated = selectedIds.contains(item.id)

                val view = this
                var clickProcessing =false

                with(checkBox){
                    visibility = if(isShowCheckBoxes) View.VISIBLE else GONE
                    setOnCheckedChangeListener {buttonView, isChecked ->  }
                    isChecked = selectedIds.contains(item.id)
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        if(isChecked) {
                            selectedIds += itemId
                            ////Log.d("#VIEW","SELECTED ${selectedPositions.joinToString ("\n")}")
                        }
                        else {
                            selectedIds -= itemId
                            ////Log.d("#VIEW", "UNSELECTED ${selectedPositions.joinToString("\n")}")
                        }
                        if(!clickProcessing)
                            selectItem(item, view, isChecked)
                    }
                }

                setClickListener {
                    if(!isContextAction) {
                        clickProcessing = true
                        selectItem(item, this, true)
                        clickProcessing = false
                        selectedId = item.id
                    }
                }

                setLongClickListener {
                    if (isContextAction)
                        false
                    else{
                        //if (selectedId != position)
                        clickProcessing = true
                        selectItem(item, this, true)
                        clickProcessing = false
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
                        if(!isContextAction) {
                            controlClickListener?.invoke(item, cv)
                        }
                    }
                }
            }
        }

        private fun selectItem(item: T, itemView: ListRowView, selected: Boolean) {

            if (selectionMode == SelectionMode.Single && !isContextAction){
                prevSelected?.isActivated = false
                prevSelected?.checkBox?.isChecked = false
                selectedId = item.id
                prevSelected = itemView
            }
            itemView.isActivated = selected
            itemView.checkBox.isChecked = selected
            selectionListener?.invoke(item, selected)
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
                ////Log.d("#NodeView","${textView.text} active: $activated")
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
        private var waitCont: Continuation<Unit>? = null

        abstract fun onError(exception: Exception)

        override fun loadInitial(
            params: LoadInitialParams,
            callback: LoadInitialCallback<T>

        ) = fetchData(0,params.requestedLoadSize, initCallback = callback)

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) =
            fetchData(params.startPosition,params.loadSize, callback = callback)

        private fun fetchData(start: Int, size: Int,
                              initCallback: LoadInitialCallback<T>? = null,
                              callback: LoadRangeCallback<T>? = null
        ){

            scope.launch(Dispatchers.IO) {

                val wc = waitCont
                var l: List<T> = emptyList()
                try {
                    ////Log.d("#DataSource","$start : $size")
                    l = loadData(start, size)
                    wc?.apply {
                        waitCont = null
                        resume(Unit)
                    }
                }catch (ex: Exception){
                    onError(ex)
                    wc?.apply {
                        waitCont = null
                        resumeWithException(ex)
                    }
                }
                val ll = l
                withContext(Dispatchers.Main) {
                    initCallback?.onResult(ll,0)
                    callback?.onResult(ll)
                }
            }

        }

        protected abstract suspend fun loadData(startPosition: Int, loadSize: Int): List<T>

        suspend fun waitDataReady(id: Long) {
            val r = suspendCoroutine<Unit> {continuation ->
                waitCont = continuation
            }
            return r
        }
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

    val coroScope
        get() =  viewModelScope

    var state: Parcelable? = null
    var liveList: MutableLiveData<PagedList<out ListRow>>? = null
    var selectionMode = ListView.SelectionMode.Single
    var selectedId: Long = -1
    var controller: ListControllerInterface? = null
    val selectedIds = mutableSetOf<Long>()
}

interface ListControllerInterface
interface ListRow {
    val id: Long
    val name: String
}

interface Clickable {
    fun clickListener(l:(rsc:View)->Unit)
}