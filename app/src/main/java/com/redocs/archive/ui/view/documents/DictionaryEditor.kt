package com.redocs.archive.ui.view.documents

import android.content.Context
import com.redocs.archive.ui.utils.CustomEditor
import com.redocs.archive.ui.view.list.FilteredList
import com.redocs.archive.ui.view.list.SimpleList

class DictionaryEditor(
    context: Context,
    value: DocumentModel.DictionaryEntry?
): FilteredList<DocumentModel.DictionaryEntry>(context,value),
    CustomEditor<DocumentModel.DictionaryEntry>
{
    override val value: DocumentModel.DictionaryEntry?
        get() {

            try {
                return selected
            }catch (ex: ArrayIndexOutOfBoundsException){
                return null
            }
        }

}