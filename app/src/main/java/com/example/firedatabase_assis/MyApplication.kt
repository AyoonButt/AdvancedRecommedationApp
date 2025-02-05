package com.example.firedatabase_assis


import android.app.Application
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.jakewharton.threetenabp.AndroidThreeTen

class MyApplication : Application() {
    lateinit var userViewModel: UserViewModel

    override fun onCreate() {
        super.onCreate()
        userViewModel = UserViewModel(this)
        AndroidThreeTen.init(this)
    }

    companion object {
        private lateinit var instance: MyApplication

        fun getInstance(): MyApplication {
            return instance
        }
    }

    init {
        instance = this
    }
}