package com.example.firedatabase_assis


import android.app.Application
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.jakewharton.threetenabp.AndroidThreeTen
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class MyApplication : Application() {
    lateinit var userViewModel: UserViewModel

    override fun onCreate() {
        super.onCreate()
        userViewModel = UserViewModel(this)
        AndroidThreeTen.init(this)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    companion object {
        private lateinit var instance: MyApplication


        lateinit var okHttpClient: OkHttpClient


        fun getInstance(): MyApplication {
            return instance
        }
    }

    init {
        instance = this
    }
}