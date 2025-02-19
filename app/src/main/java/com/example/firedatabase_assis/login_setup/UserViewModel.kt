package com.example.firedatabase_assis.login_setup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.UserEntity
import com.example.firedatabase_assis.postgres.UserUpdate
import com.example.firedatabase_assis.postgres.Users
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentUser = MutableLiveData<UserEntity?>()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: Users = retrofit.create(Users::class.java)

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

    suspend fun updateUserProfile(userId: Int, updates: UserUpdate): Result<UserEntity> {
        return try {
            val response = api.updateUser(userId, updates)
            if (response.isSuccessful) {
                response.body()?.let { userEntity ->
                    _currentUser.value = userEntity
                    Result.success(userEntity)
                } ?: Result.failure(Exception("Response body was null"))
            } else {
                Result.failure(Exception("Failed to update user: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating user profile: ${e.message}")
            Result.failure(e)
        }
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