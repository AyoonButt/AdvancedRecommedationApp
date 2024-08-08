package com.example.firedatabase_assis.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {

    private val _selectedItem = MutableLiveData<Any>()
    val selectedItem: LiveData<Any> get() = _selectedItem

    fun setSelectedItem(item: Any) {
        _selectedItem.value = item
    }

}




