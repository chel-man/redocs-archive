package com.redocs.archive.ui.view.tabs

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.redocs.archive.R
import com.redocs.archive.ui.ContextActionModeController
import com.redocs.archive.ui.ContextActionBridge


class TabBarView(
    parent: Fragment,
    tabs: List<Tab>

) : ViewPager(parent.context as Context){

    var selected: TabBarView.Tab? = null
    var selectionListener: ((selected: Tab?, toSelect: Tab) -> Unit)? = null

    private var tabsHeader: TabBarView.Header
    private val pl = object:SimpleOnPageChangeListener(){
        override fun onPageSelected(position: Int) {
            val tab = (adapter as Adapter).getTab(position)
            //Log.d("#TABS","selecting $position [${tab.title}] selected: $selected  tab: $tab")
            //selectionListener?.invoke(selected,tab)
            selected = tab
        }
    }

    init {
        tag="${this}"
        this.id= getNextId(parent)
        ////Log.d("TabBarView","CREATED $id tag: $tag")
        tabsHeader = parent.layoutInflater.inflate(R.layout.tab_bar_header,this,false) as Header
        adapter = Adapter(context as Context, parent.childFragmentManager,tabs)//titles)
        tabsHeader.setupWithViewPager(this)
        this.addView(tabsHeader)
        ////Log.d("TabBarView","CREATED WITH ARGS")
        addOnPageChangeListener(pl)
    }

    var isHidded: Boolean = false
        set(value) {
            tabsHeader.apply{
                //isEnabled = hidden
                visibility = if (!value) View.VISIBLE else View.GONE
            }
        }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if(isEnabled)
            return super.onTouchEvent(ev)
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if(isEnabled)
            return super.onInterceptTouchEvent(event)
        return false
    }

    private inner class Adapter(
        private val context: Context,
        fm: FragmentManager,
        private val tabs: List<Tab>

    ) : FragmentPagerAdapter(fm,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {

        override fun getCount(): Int {
            return tabs.size
        }

        override fun getItem(position: Int): Fragment {
            var f = createFragment(position)
            Log.d("#PageAdapter","created $position")
            return f
        }

        private fun createFragment(position: Int): Fragment {
            val tab=tabs[position]
            val f = tab.createFragment()
            if(selected == null && position == 0)
                selected = tab
            return f
        }

        private fun setupFragment(position: Int, f: Fragment){
            val tab=tabs[position]
            tab.setupFragment(f)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return context.getString(tabs[position].title)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            var f= super.instantiateItem(container, position) as Fragment
            Log.d("#PageAdapter","instantiated $position $f")
            setupFragment(position, f)
            return f
        }

        fun getTab(position: Int): Tab {
            return tabs[position]
        }
    }

    class Header(context: Context, attrs: AttributeSet?): TabLayout(context,attrs) {

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)
            if(childCount>0){
                val vg=getChildAt(0) as ViewGroup
                for(t in vg.children) {
                    t.isEnabled=enabled
                }
            }
        }

    }

    class Tab(
        val title: Int,
        createFragment: () -> Fragment,
        setupFragment: (Fragment) -> Unit,
        //action: (Boolean)->Unit
        ac: ContextActionModeController?
    ){
        lateinit var fragment: Fragment

        init{
            //Log.d("#TAB","CREATED $title")
        }

        val createFragment = {
            fragment = createFragment()
            //Log.d("#TAB","CREATED $title")
            fragment
        }

        val setupFragment = { fragment: Fragment ->
            /*if(fragment is ContextActionBridge) {
                fragment.actionListener = action
            }*/
            setupFragment(fragment)
            Log.d("#TAB","SETUP $fragment")
            if(ac !=null)
                (fragment as ContextActionBridge).contextActionModeController = ac
            //Log.d("#TAB","FRAGMENT CONFIGURED $title")
            this.fragment = fragment
        }
    }
}

/*****************************************************************
                            DSL
 *****************************************************************/
class TabBarViewBuilder(private val parent: Fragment) {

    private val tabs= mutableListOf<TabBarView.Tab>()

    fun tab(init: TabBuilder.()->Unit){
        var tb=TabBuilder()
        tb.init()
        tabs.add(tb.build())
    }

    fun build(): TabBarView {
        return TabBarView(parent,tabs)
    }

    class TabBuilder {
        private lateinit var fragmentBuilder: FragmentBuilder
        private var title: Int? = null

        fun title(fn: ()->Int){
            title=fn()
        }

        fun fragment(init: FragmentBuilder.()->Unit){
            fragmentBuilder=FragmentBuilder(this)
            fragmentBuilder.init()
        }

        fun build(): TabBarView.Tab {
            ////Log.d("#TabBuilder","TAB $title CREATED")
            return TabBarView.Tab(
                title as Int,
                fragmentBuilder.create,
                fragmentBuilder.setup,
                //fragmentBuilder.action
                fragmentBuilder.contextActionModeController)
        }
    }

    class FragmentBuilder(private val tb: TabBuilder) {

        var contextActionModeController: ContextActionModeController? = null
        //var action: (Boolean) -> Unit = {}
        var setup: (Fragment) -> Unit = {}
        lateinit var create: () -> Fragment

        fun onCreate(l: ()->Fragment) {
            create=l
        }

        fun onSetup(l: (Fragment)->Unit) {
            setup= l
        }

        /*fun onActionModeChanged(l: (Boolean)->Unit) {
            action=l
        }*/

    }
}

private val ids= mutableMapOf<String, Int>()

private fun getNextId(f: Fragment): Int {

    val key = f.javaClass.toString()
    var id = ids.get(key)
    id = id ?: View.generateViewId()
    ids[key]=id
    return id
}

fun Fragment.tabBar(init: TabBarViewBuilder.()->Unit): TabBarView {
    var tb=TabBarViewBuilder(this)
    tb.init()
    return tb.build()
}