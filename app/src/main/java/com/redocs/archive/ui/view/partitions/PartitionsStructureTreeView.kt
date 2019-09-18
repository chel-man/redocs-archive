package com.redocs.archive.ui.view.partitions

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import com.redocs.archive.R
import com.redocs.archive.data.PartitionStructureRepository
import com.redocs.archive.data.partitions.PartitionsStructureRepository
import com.redocs.archive.domain.TreeNode
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.ui.ContextActionModeController
import com.redocs.archive.ui.ContextActionSource
import com.redocs.archive.ui.events.SelectPartitionNodeRequestEvent
import com.redocs.archive.ui.setItemEnabled
import com.redocs.archive.ui.utils.convertDpToPixel
import com.redocs.archive.ui.view.tree.TreeView
import com.redocs.archive.ui.view.tree.TreeViewViewModel
import com.redocs.archive.ui.view.tree.TreeViewNode
import kotlinx.coroutines.CoroutineScope

class PartitionsStructureTreeView(
    context: Context,
    vm: TreeViewViewModel,
    repository: PartitionsStructureRepository

): TreeView<PartitionStructureTreeViewNode>(context, vm), ContextActionSource, EventBusSubscriber {
    override val lockContent = true

    var contextActionModeController: ContextActionModeController? = null
    var nodeActionListener: ((id: Long, action: Action)->Unit)? = null

    private val contextMenuIdRes: Int = R.menu.partitions_context_menu
    private var controller: PartitionsStructureTreeView.PartitionStructureTreeController? = null
    private var menu: Menu? = null

    init {
        //subscribe(SelectPartitionNodeRequestEvent::class)
        /* Controller created in createController() called from base class before
            call this constructor.
            Not must save ref to it
         */
        controller?.repo = repository
        controller = null
        longClickListener = {
            if(allowContextActionMode())
                contextActionModeController?.startActionMode(this)
            true
        }
    }

    /*override fun createNode(id: Int, text: String): TreeViewNode =
        PartitionStructureTreeViewNodeImpl(id,text,false)*/

    override fun createNodeView(context: Context): TreeNodeView {
        val v = super.createNodeView(context)
        v.addView(
            ImageView(context).apply {
                setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.ic_list_24dp
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
            })
        return v
    }

    override fun nodeViewBinded(node: TreeViewNode, view: TreeNodeView) {

        val c = (view as ViewGroup).getChildAt(view.childCount-1)
        if(node.id == 0L)
            c.visibility = View.INVISIBLE
        else
            c.visibility = View.VISIBLE

        c.setOnClickListener {
            nodeActionListener?.invoke(node.id,Action.VIEW)
        }
    }

    override suspend fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is SelectPartitionNodeRequestEvent -> { select(evt.data as Long)}
        }
    }

    override fun createController(
        ld: MutableLiveData<DataModel<PartitionStructureTreeViewNode>>,
        scope: CoroutineScope
    ): TreeController<PartitionStructureTreeViewNode> {
        val lc = PartitionStructureTreeController(ld,scope)
        controller = lc
        return lc
    }

    override fun createContextActionMenu(inflater: MenuInflater, menu: Menu) {
        isEnabled =false
        inflater.inflate(contextMenuIdRes,menu)
        configureContextActionMenu(menu)
    }

    override fun onDestroyContextAction() {
        isEnabled = true
        menu = null
    }

    override fun onContextMenuItemClick(mode: ActionMode, item: MenuItem?): Boolean {
        mode.finish()
        return true
    }

    override fun allowContextActionMode(): Boolean {
        return selected?.id != -1L
    }

    private fun configureContextActionMenu(menu: Menu) {
        this.menu=menu
        val node = selected
        if(node != null) {
            menu.apply {
                val id=node.id
                val dc = Color.GRAY//ContextCompat.getColor(context, R.color.dimmed)
                setItemEnabled(R.id.ps_context_delete,id > 0, dc)
                setItemEnabled(R.id.ps_context_add_next,id > 0,dc)
            }
        }
    }

    private class PartitionStructureTreeViewNodeImpl(id: Long, text: String, isLeaf: Boolean) :
        PartitionStructureTreeViewNode, TreeView.TreeViewNodeBase(id, text, isLeaf, 0, false)

    private class PartitionStructureTreeController (
        //private val repo: PartitionStructureRepository<out TreeNode>,
        liveModel: MutableLiveData<DataModel<PartitionStructureTreeViewNode>>,
        scope: CoroutineScope
    ) : TreeController<PartitionStructureTreeViewNode>(/*repo,*/ liveModel, scope) {

        lateinit var repo: PartitionStructureRepository<out TreeNode>

        private fun toTreeVeiwNode(n: TreeNode): PartitionStructureTreeViewNode =
            PartitionStructureTreeViewNodeImpl(
                n.id,
                n.name,
                n.isLeaf
            )

        override suspend fun getNode(id: Long): PartitionStructureTreeViewNode =
                toTreeVeiwNode(repo.get(id))

        override suspend fun getChildren(id: Long): List<PartitionStructureTreeViewNode> =
            repo.getChildren(id).map(::toTreeVeiwNode )

        override suspend fun getPath(id: Long): List<Long> =
            repo.getPath(id)
    }
}



interface PartitionStructureTreeViewNode : TreeViewNode

class PartitionStructureViewModel : TreeViewViewModel() {
    var repository: PartitionsStructureRepository? = null
}

enum class Action {
    VIEW
}