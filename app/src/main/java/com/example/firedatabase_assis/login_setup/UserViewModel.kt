package com.example.firedatabase_assis.login_setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.firedatabase_assis.postgres.UserEntity


class UserViewModel : ViewModel() {

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    fun setUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun clearUser() {
        _currentUser.value = null
    }

    // Getter for userId
    fun getUserId(): Int? {
        return _currentUser.value?.userId
    }

    // Getter for language
    fun getLanguage(): String? {
        return _currentUser.value?.language
    }
}
