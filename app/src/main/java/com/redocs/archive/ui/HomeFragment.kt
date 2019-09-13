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
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.redocs.archive.R
import com.redocs.archive.framework.InMemoryDocumentsDataSource
import com.redocs.archive.framework.InMemoryPartitionsStructureDataSource
import com.redocs.archive.ui.view.ActivablePanel
import com.redocs.archive.ui.view.tabs.*
import kotlinx.android.synthetic.main.home_fragment.*


class HomeFragment : Fragment(), ActivablePanel {

    override var isActive = false
    private lateinit var tabs: TabBarView

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

        tabs=tabBar {
                tab {
                    title { R.string.structure_tab_title }
                    fragment {
                        onCreate {
                            StructureFragment(InMemoryPartitionsStructureDataSource())
                        }
                        contextActionModeController = ContextContextActionInterceptor(
                            (activity as ContextActionModeController))
                    }
                }
                tab {
                    title { R.string.documents_tab_title }
                    fragment {
                        onCreate {
                            DocumentsFragment(InMemoryDocumentsDataSource())
                        }
                        contextActionModeController = ContextContextActionInterceptor(
                            (activity as ContextActionModeController))
                    }
                }
                tab {
                    title { R.string.files_tab_title }
                    fragment {
                        onCreate {
                            FilesFragment()
                        }
                    }
                }
            }

        tabs.selectionListener = ::tabSelected

        (view as ViewGroup).addView(tabs)

    }

    override fun activate() {
    }

    override fun deactivate() {
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
    }

    private inner class ContextContextActionInterceptor(
        private val controllerContext: ContextActionModeController

    ) : ContextActionModeController {

        override fun startActionMode(source: ContextActionSource) {

            class ContextActionSourceInterceptor:ContextActionSource by source {
                override fun onDestroyContextAction() {
                    source.onDestroyContextAction()
                    dimmer.isVisible = false
                    tabs.isHidded = false
                }
            }
            controllerContext.startActionMode(ContextActionSourceInterceptor())

            dimmer.isVisible = source.lockContent
            if(dimmer.isVisible)
                dimmer.bringToFront()
            tabs.isHidded = true
        }
    }

}

private fun tabSelected(prevTab: TabBarView.Tab?, tab: TabBarView.Tab) {
    /*Log.d("#TABS","SELECTED ${tab.title}")
    (prevTab?.fragment as? ActivablePanel)?.deactivate()
    (tab.fragment as? ActivablePanel)?.activate()
     */
}