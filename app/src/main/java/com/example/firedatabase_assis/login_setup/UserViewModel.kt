package com.example.firedatabase_assis.login_setup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firedatabase_assis.postgres.UserEntity

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    fun setUser(user: UserEntity) {
        Log.d("UserViewModel", "Setting user: $user")
        _currentUser.value = user
    }

    fun getUser(): UserEntity? {
        Log.d("UserViewModel", "Getting user: ${_currentUser.value}")
        return _currentUser.value
    }

    fun clearUser() {
        _currentUser.value = null
    }

    fun getUserId(): Int? {
        return _currentUser.value?.userId
    }

    fun getLanguage(): String? {
        return _currentUser.value?.language
    }

    companion object {
        private var instance: UserViewModel? = null

        fun getInstance(application: Application): UserViewModel {
            return instance ?: synchronized(this) {
                instance ?: UserViewModel(application).also { instance = it }
            }
        }
    }
}