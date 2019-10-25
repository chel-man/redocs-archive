/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redocs.archive.ui

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.redocs.archive.R
import com.redocs.archive.framework.EventBus
import com.redocs.archive.framework.EventBusSubscriber
import com.redocs.archive.framework.subscribe
import com.redocs.archive.isParentOf
import com.redocs.archive.ui.events.*
import com.redocs.archive.ui.utils.ActivablePanel
import com.redocs.archive.ui.utils.ContextActionSource
import com.redocs.archive.ui.view.tabs.TabBarView
import com.redocs.archive.ui.view.tabs.tabBar
import kotlinx.android.synthetic.main.home_fragment.*


class HomeFragment : Fragment(), EventBusSubscriber {

    private var restored = false
    private lateinit var tabs: TabBarView

    init {
        subscribe(
            ActivateDocumentListEvent::class.java,
            DocumentSelectedEvent::class.java,
            ShowDocumentEvent::class.java,
            ContextActionRequestEvent::class.java,
            ContextActionStoppedEvent::class.java)
    }

    override fun onEvent(evt: EventBus.Event<*>) {
        when(evt){
            is ActivateDocumentListEvent ->
                tabs.selectTab(1)

            is ShowDocumentEvent -> {
                Handler().post {
                    tabs.selectTab(2)
                }
            }

            is ContextActionRequestEvent -> ContextActionStarted(evt.data)
            is ContextActionStoppedEvent -> ContextActionStopped(evt.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabs=createTabBar(::tabSelected)
        getRootLayout().addView(tabs)

    }

    private fun createTabBar(tabSelected: (TabBarView.Tab?, TabBarView.Tab)->Unit): TabBarView =
        tabBar {
            tab {
                title { R.string.structure_tab_title }
                fragment {
                    onCreate {
                        StructureFragment()
                    }
                }
            }
            tab {
                title { R.string.documents_tab_title }
                fragment {
                    onCreate {
                        DocumentListFragment()
                    }
                }
            }
            tab {
                title { R.string.documents_tab_document }
                fragment {
                    onCreate {
                        DocumentDetaileFragment()
                    }
                }
            }
            tab {
                title { R.string.links_tab_title }
                fragment {
                    onCreate {
                        LinksFragment()
                    }
                }
            }
        }.apply {
            selectionListener = tabSelected
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    private fun ContextActionStarted(source: ContextActionSource){
        if(isParentOf(source as View)) {
            dimmer.isVisible = source.lockContent
            tabs.isHidded = true
        }
    }

    private fun ContextActionStopped(source: ContextActionSource){
        if(isParentOf(source as View)) {
            dimmer.isVisible = false
            tabs.isHidded = false
        }
    }

    /*override fun onBackPressed(): Boolean {

        with(getRootLayout()){
            val fv = getChildAt(0)
            if(fv is DocumentFragment.FieldListView) {
                if(fv.allowClose()) {
                    removeView(fv)
                    /*if(restored) {
                        restored = false*/
                        tabs=createTabBar(::tabSelected)
                        /*Handler().post{
                            tabs.selectTab(0)
                        }*/
                    //}
                    addView(tabs)
                    dvm.document = null
                    return true
                }
            }
        }
        return false
    }*/

    private fun getRootLayout() = home_root_view
}

private fun tabSelected(prevTab: TabBarView.Tab?, tab: TabBarView.Tab) {
    try {
        Handler().post {
            Log.d("#TABS","DEselected ${prevTab?.title}")
            (prevTab?.fragment as? ActivablePanel)?.deactivate()
        }
        Handler().post {
            Log.d("#TABS","SElected ${tab.title}")
            (tab.fragment as? ActivablePanel)?.activate()
        }
    }catch (ex: UninitializedPropertyAccessException){}
}