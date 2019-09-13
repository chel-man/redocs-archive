package com.redocs.archive.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redocs.archive.data.partitions.PartitionsStructureRepository
import com.redocs.archive.ui.view.tree.TreeControllerInterface

/*class PartitionsStructureViewModel: ViewModel(), TreeViewViewModel {

    override val coroScope
            get() =  viewModelScope

    var repository: PartitionsStructureRepository? = null
    override var controller: TreeControllerInterface? = null
    override val data: MutableLiveData<TreeViewDataModel> = MutableLiveData()
}*/