package com.redocs.archive.ui.view.tree

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.redocs.archive.R
import com.redocs.archive.ui.utils.*
import kotlinx.coroutines.*

abstract class TreeView<T : TreeViewNode>(
    context: Context,
    vm: TreeViewViewModel
) : RecyclerView(context){

    var selected: T? = null
        get() = (adapter as Adapter<T>).selected

    var longClickListener: ((node: T) -> Boolean)? = null

    private val selectionListeners: MutableList<(node: T?) -> Unit> = mutableListOf()

    init {
        val data = vm.data.value ?: createDataModel()
        vm.data.value=data
        val dataModel=vm.data as MutableLiveData<DataModel<T>>
        val ctrl = vm.controller as TreeController<T>? ?:
                createController(dataModel,vm.coroScope)
        vm.controller = ctrl
        layoutManager = LinearLayoutManager(context)
        adapter = Adapter(context,this, ctrl,dataModel).apply {

                selectionListener = ::onSelectionChanged
                longClickListener = ::onLongClick
            }

        addOnItemTouchListener(object : OnItemTouchListener {
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            }

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return !this@TreeView.isEnabled
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            }
        })

    }

    protected open fun createDataModel(): DataModel<T> {
        return DataModel()
    }

    protected open fun createController(ld: MutableLiveData<DataModel<T>>, scope: CoroutineScope) : TreeController<T> {
        return TreeController(ld,scope)
    }

    fun select(id:Long){
        (adapter as Adapter<T>).select(id)
    }

    protected fun getNodeImageId(node: T): Int {
        return TreeViewImages().getImage(node)
    }

    private fun onLongClick(node: T): Boolean {
        val l = longClickListener
        if(l != null)
            return l(node)
        else
            return false
    }

    private fun onSelectionChanged(node: T?) {
        for (l in selectionListeners)
            l(node)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val apos=(adapter as Adapter<*>).firstItemPosition
        val pos = (layoutManager as LinearLayoutManager)
            .findFirstCompletelyVisibleItemPosition()
        if(apos != pos)
            (adapter as Adapter<*>).firstItemPosition = pos
    }

    fun refresh() {

        (adapter as Adapter<T>).apply {
            refresh()
        }
    }

    fun addSelectionListener(l: (node: T?) -> Unit) {
        selectionListeners.add(l)
    }

    /*protected open fun createNode(id: Int, text: String): TreeViewNode =
        TreeViewNodeBase(id,text,false,0,false)*/

    protected open class TreeViewNodeBase(
        override val id: Long,
        override val text: String,
        override val isLeaf: Boolean,
        override var level: Int,
        override var opened: Boolean
    ) : TreeViewNode {

        override var enabled = true
        override var loading = false

        override fun toString(): String {
            return "$id : $text : $loading"
        }
    }

    private class Adapter<T : TreeViewNode>(
        private val context: Context,
        private val tree: TreeView<T>,
        private val controller: TreeController<T>,
        ld: LiveData<DataModel<T>>

    ) : RecyclerView.Adapter<TreeView.Adapter.TreeItemViewHolder>() {
        var selected: T?
            get() {
                if (model.selectedPosition > -1)
                    return model.data[model.selectedPosition]
                return null
            }
            private set(value) {}

        var firstItemPosition: Int
            get() = model.firstItemPosition
            set(value) {
                controller.firstItemPosition = value
            }

        lateinit var longClickListener: (node: T) -> Boolean
        lateinit var selectionListener: (node: T?) -> Unit

        private var prevSelected: TreeNodeView? = null
        private var model = DataModel<T>()
        private var firstRun = true

        init {
            with(ld){
                removeObservers(context as LifecycleOwner)
                observe(context as LifecycleOwner, Observer<DataModel<T>> {
                    var submit = true
                    if (firstRun) {
                        firstRun = false
                        if (it.data.isEmpty()) {
                            Handler().post { reload() }
                            submit = false
                        }
                        else{
                            // Scroll to remembered position
                            Handler().post {
                                if(it.firstItemPosition > 0)
                                    tree.scrollToPosition(it.firstItemPosition)
                            }
                        }
                    }
                    if(submit){
                        model = it
                        submitOperation(model.operation)
                    }
                })
            }
        }

        private fun submitOperation(op: Operation){
            if(!op.proccessed) {
                when (op) {
                    is Error -> showError(context, op.ex)
                    is UpdateOperation -> notifyItemChanged(op.pos)
                    is InsertOperation -> notifyItemRangeInserted(op.pos, op.size)
                    is RemoveOperation -> notifyItemRangeRemoved(op.pos, op.size)
                }
                op.proccessed = true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TreeItemViewHolder(tree.createNodeView(context))

        override fun getItemCount() = model.data.size

        override fun onBindViewHolder(holder: TreeItemViewHolder, position: Int) {

            val node = model.data[position]

            (holder.itemView as TreeNodeView).apply {

                text = node.text
                level = node.level

                if(node.loading)
                    isLoading = true
                else {
                    isLoading = false
                    image = tree.getNodeImageId(node)
                }

                if(prevSelected == this)
                    prevSelected = null

                if (model.selectedPosition == position) {
                    isActivated = true
                    prevSelected = this
                }
                else
                    isActivated = false

                val thiz = this

                setTextClickListener {
                    selectNode(node, thiz, position)
                }

                setTextLongClickListener {
                    if (model.selectedPosition != position)
                        selectNode(node, thiz, position)
                    longClickListener(node)
                }

                if (!node.isLeaf && node.enabled) {
                    setImageClickListener {
                        if(node.enabled) {
                            if (node.opened)
                                controller.collapse(node, position)
                            else
                                controller.expand(node, position)
                        }
                    }
                }

                tree.nodeViewBinded(node,this)
            }

        }

        private fun selectNode(node: T, nodeView: TreeNodeView, pos: Int) {
            //Log.d("#ListAdapter","select node prev: ${prevSelected?.text}")
            if (prevSelected !== nodeView) {
                controller.selectedPosition = pos
                selectionListener(if (pos > -1) node else null)
            }
        }

        fun clearSelection() {
            prevSelected?.isActivated = false
            prevSelected = null
            controller.selectedPosition = -1
            //Log.d("#ListAdapter","clear selection")
            selectionListener.invoke(null)
        }

        private fun reload() {
            controller.clear()
            clearSelection()
            var node = LoadingNode as T
            node.loading = true
            controller.add(node)
            controller.expand(node, 0)
        }

        fun refresh() {
            reload()
        }

        fun select(id: Long) {
            controller.select(id)
        }

        private class TreeItemViewHolder(itemView: TreeNodeView) : RecyclerView.ViewHolder(itemView)
    }

    protected abstract fun nodeViewBinded(node: TreeViewNode, treeNodeView: TreeView.TreeNodeView)

    protected open fun createNodeView(context: Context): TreeView.TreeNodeView {
        return TreeNodeView(context)
    }

    protected class TreeNodeView(context: Context) : LinearLayoutCompat(context) {

        private object Conf {
            val textTypeFace = Typeface.NORMAL
            val backgroundColor = Color.TRANSPARENT
            val levelIndent = 24
            @ColorRes
            var textColor = R.color.colorPrimaryDark
            //val textSize = 20.0F
            @ColorRes
            var selectedBgColor = R.color.colorPrimary
            val selectedTextColor = Color.WHITE
            val selectedTextTypeface = Typeface.BOLD
        }

        private val padding: Int = convertDpToPixel(5, context)
        private val dp48: Int = convertDpToPixel(48, context)

        private val imageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                dp48,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER
        }

        private val textView = TextView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1F
            )
            gravity = Gravity.CENTER_VERTICAL
            //textSize = Conf.textSize
            setPadding(padding, 0, padding, 0)
            setTextColor(Conf.textColor)
        }

        @DrawableRes
        var image: Int = 0
            set(value) {
                imageView.setImageDrawable(AppCompatResources.getDrawable(context, value))
            }

        var text = "Text PlaceHolder"
            set(value) {
                textView.text = value
            }

        var level: Int = 0
            set(value) {
                field = value
                imageView.layoutParams = LayoutParams(
                    imageView.layoutParams.width,
                    imageView.layoutParams.height
                ).apply {
                    leftMargin = Conf.levelIndent * value
                }
            }

        var isLoading: Boolean = false
            set(value){
                removeViewAt(0)
                if(value)
                    addView(ProgressBar(context, null,android.R.attr.progressBarStyleSmall).apply{
                            this.isIndeterminate = true
                            layoutParams = LayoutParams(dp48,dp48).apply {
                                leftMargin = Conf.levelIndent * level
                            }

                        },0)
                else
                    addView(imageView,0)

            }

        init {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )

            if (Conf.selectedBgColor > 0) {
                Conf.selectedBgColor = ContextCompat.getColor(context, Conf.selectedBgColor)
                Conf.textColor = ContextCompat.getColor(context, Conf.textColor)
            }
            addView(imageView)
            addView(textView)
        }

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)
            textView.isEnabled = enabled
        }

        /*override fun generateDefaultLayoutParams(): LayoutParams? {
            return LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }*/

        fun setImageClickListener(l: (view: View) -> Unit) {
            imageView.setOnClickListener(l)
        }

        fun setTextClickListener(l: (view: View) -> Unit) {
            textView.setOnClickListener(l)
        }

        fun setTextLongClickListener(l: (view: View) -> Boolean) {
            textView.setOnLongClickListener(l)
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

            textView.apply {
                setBackgroundColor(bgc)
                setTextColor(fc)
                setTypeface(null, tf)
            }
            //Log.d("#NodeView","${textView.text} active: $activated")
        }
    }

    private class TreeNodeStub(
        val parent: TreeNodeStub?,
        id: Long,
        text: String,
        val children: MutableList<TreeNodeStub> = mutableListOf<TreeNodeStub>()
    ) : TreeViewNodeBase(id,text,true,0,false)

    protected open class TreeController<T : TreeViewNode>(
        //private val repo: PartitionStructureRepository<out TreeNode>,
        private val liveModel: MutableLiveData<DataModel<T>>,
        private val scope: CoroutineScope
    ) : TreeControllerInterface {

        private val data = createEmptyData() as TreeNodeStub
        private val ld=(liveModel.value as DataModel<T>)
        var firstItemPosition = ld.firstItemPosition
            set(value) {
                field = value
                notifyObservers(Empty())
            }

        var selectedPosition = ld.selectedPosition
            set(value) {
                val pp = field
                field = value
                if(pp > -1)
                    notifyObservers(UpdateOperation(pp))
                notifyObservers(UpdateOperation(value))
            }

        private val items = mutableListOf<T>().apply {
            addAll(ld.data)
        }

        protected fun createEmptyData(): TreeViewNode {
            return TreeNodeStub(null,0,"Root")
        }

        /***************************************
            Methods for override
        ****************************************/
        protected open suspend fun getNode(id: Long): T {
            return findInData(listOf(data),id) as T
        }

        protected open suspend fun getChildren(id: Long): List<T> {
            return (getNode(id) as TreeNodeStub).children as List<T>
        }

        protected open suspend fun getPath(id: Long): List<Long> {

            val l = mutableListOf(id)
            var node = getNode(id) as TreeNodeStub
            while(node.id > 0) {
                l.add(node.id)
                node = node.parent as TreeNodeStub
            }
            return l
        }
        /***************************************
            Methods for override END
         ****************************************/

        private fun findInData(nodes: List<TreeNodeStub>, id: Long, level: Int=0): TreeNodeStub? {
            for (n in nodes) {
                if (n.id == id)
                    return n
                val nn = findInData(n.children, id, level + 1)
                if (nn != null)
                    return nn
            }
            if (level == 0)
                throw Exception("Раздел с ID=$id не найден")
            return null

        }

        fun clear() {
            items.clear()
        }

        @Deprecated("TODO")
        fun add(node: T) {
            items.add(node)
            notifyItemRangeInserted(items.size-1,1)
        }

        private fun getPosition(node: T): Int {
            val index=items.indexOf(node)
            if(index<0)
                throw NotFoundException(node.id, "getPosition")
            return index
        }

        private fun addChildren(node: T, nodes: List<T>) {
            val position = getPosition(node) + 1
            items.addAll(position, nodes)
        }

        fun collapse(
            node: T,
            position: Int
        ) {
            if(selectedPosition> position){
                val pos = selectedPosition
                selectedPosition = -1
                notifyItemChanged(pos)
            }
            val count = removeChildren(node)
            node.opened = false
            notifyItemRangeRemoved(position + 1, count)
            notifyItemChanged(position)
        }

        private fun removeChildren(node: T): Int {

            val position = getPosition(node) + 1
            val currLevel = node.level
            var count = 0

            while (position < items.size) {
                val item = items[position]
                if (item.level == currLevel)
                    break
                items.removeAt(position)
                count++
            }
            return count
        }

        fun expand(
            node: T,
            position: Int
        ) {
            node.apply {
                loading = true
                enabled = false
            }

            scope.launch {
                expandInternal(position)
            }
            notifyItemChanged(position)
        }

        private suspend fun expandInternal(
            position: Int
        ): Int {
            val node = items[position]
            var error: Exception? = null
            var nn = node
            var count = -1
            try {
                val items = withContext(Dispatchers.IO) {
                    nn = replace(getNode(node.id)/*toTreeVeiwNode(
                                    repo.get(node.id)
                                )*/, position)
                    nn.level = node.level
                    nn.enabled = node.enabled
                    nn.loading = node.loading
                    /*repo.*/getChildren(node.id)/*.map { toTreeVeiwNode(it) }*/
                }

                withContext(Dispatchers.Default) {
                    items.map { it.level = node.level + 1 }
                    addChildren(nn, items)
                }
                nn.opened = true
                notifyItemRangeInserted(position + 1, items.size)
                count = items.size
            } catch (jce: CancellationException) {
                //Log.d("#TreeController", "expand CANCELLED")
            } catch (ex: java.lang.Exception) {
                error = ex
            } finally {
                nn.apply {
                    loading = false
                    enabled = true
                }
            }
            if (error != null)
                notifyError(error)
            else
                notifyItemChanged(position)
            return count
        }

        private fun notifyItemChanged(pos: Int){
            notifyObservers(UpdateOperation(pos))
        }

        private fun notifyItemRangeInserted(pos: Int, size: Int){
            notifyObservers(InsertOperation(pos, size))
        }

        private fun notifyItemRangeRemoved(pos: Int, size: Int) {
            notifyObservers(RemoveOperation(pos, size))
        }

        private fun notifyObservers(op: Operation) {
            liveModel.value = DataModel<T>(selectedPosition,firstItemPosition, items.toList(),op)
        }

        private fun notifyError(ex: Exception){
            notifyObservers(Error(ex))
        }

        fun refresh() {
            //notifyObservers()
        }

        private fun replace(node: T, position: Int): T {
            if (position < items.size) {
                items.removeAt(position)
                items.add(position, node)
                return node
            } else
                throw NotFoundException(position.toLong(), "replace")
        }

        private fun find(id: Long): Int {
            var pos = 0
            items.forEach {
                if(it.id == id)
                    return@forEach
                pos++
            }
            return if(pos == items.size) -1 else pos
        }

        fun select(id: Long) {
            scope.launch(Dispatchers.Default) {
                var pos = find(id)
                if (pos == -1) {
                    var size = items.size
                    clear()
                    selectedPosition = -1
                    notifyItemRangeRemoved(0,size)
                    add(LoadingNode as T)
                    size = 0
                    try {
                        val path = withContext(Dispatchers.IO) {
                            /*repo.*/getPath(id)
                        }

                        for (pid in path) {
                            if (pid != id) {
                                pos = find(pid)
                                if (pos > -1) {
                                    size = expandInternal(pos)
                                }
                                if (pos == -1 || size < 1) {
                                    notifyError(
                                        NotFoundException(
                                            pid,
                                            "select()"
                                        )
                                    )
                                    break
                                }
                            }
                        }
                        if (size > 0)
                            selectedPosition = find(id)
                    }catch (ex: Exception){
                        notifyError(ex)
                    }
                } else
                    selectedPosition = pos
            }
        }

    }

    companion object {
        val LoadingNode = object:TreeViewNode {
            override val id = 0L
            override val text = "Загрузка ..."
            override val isLeaf = false
            override var level = 0
            override var opened = false
            override var enabled = false
            override var loading = true
        }
    }

    class DataModel<T: TreeViewNode>(
        val selectedPosition: Int = -1,
        val firstItemPosition: Int = 0,
        val data: List<T> = listOf<T>(),
        val operation: Operation = Empty()
    )
    {
        override fun toString() = "$operation $data"
    }

    open class TreeViewImages {

        fun getImage(viewNode: TreeViewNode): Int {

            return if(viewNode.loading)
                LOADING
            else if (viewNode.isLeaf)
                LEAF
            else {
                if (viewNode.opened)
                    OPENED
                else
                    CLOSED
            }
        }

        @DrawableRes
        protected var CLOSED = R.drawable.ic_folder_black_24dp
        @DrawableRes
        protected var OPENED = R.drawable.ic_folder_open_black_24dp
        @DrawableRes
        protected var LEAF =  R.drawable.ic_tree_leaf_black_24dp
        @DrawableRes
        protected var LOADING = R.drawable.ic_donut_large_black_24dp
    }

}

open class TreeViewViewModel: ViewModel() {

    val coroScope
        get() =  viewModelScope

    var controller: TreeControllerInterface? = null
    val data: MutableLiveData<TreeView.DataModel<out TreeViewNode>> = MutableLiveData()
}

interface TreeControllerInterface

interface TreeViewNode {
    val id: Long
    val text: String
    val isLeaf: Boolean
    var level: Int
    var opened: Boolean
    var enabled: Boolean
    var loading: Boolean
}