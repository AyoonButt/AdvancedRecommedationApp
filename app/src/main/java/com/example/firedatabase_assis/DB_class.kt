package com.example.firedatabase_assis

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DB_class(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LoginDatabase"
        internal const val TABLE_CONTACTS = "user"
        internal const val KEY_NAME = "name"
        private const val KEY_UNAME = "username"
        private const val KEY_PSWD = "pswd"
        private const val GENRES_LIST = "genres" // New column for storing preferences
        private const val SUBSCRIPTION_LIST = "subscriptions"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val newtb = ("CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_NAME + " TEXT,"
                + KEY_UNAME + " TEXT,"
                + KEY_PSWD + " TEXT,"
                + GENRES_LIST + " TEXT,"
                + SUBSCRIPTION_LIST + " TEXT"
                + ")")
        db?.execSQL(newtb)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    fun updateGenresList(username: String, genresList: MutableList<String>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(GENRES_LIST, genresList.joinToString(","))
        val success = db.update(TABLE_CONTACTS, values, "$KEY_UNAME=?", arrayOf(username))
        db.close()
        return success != -1
    }

    fun updateServicesList(username: String, servicesList: MutableList<String>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(SUBSCRIPTION_LIST, servicesList.joinToString(","))
        val success = db.update(TABLE_CONTACTS, values, "$KEY_UNAME=?", arrayOf(username))
        db.close()
        return success != -1
    }
}
