package com.example.firedatabase_assis.login_setup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DB_class(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        internal const val DATABASE_VERSION = 1
        internal const val DATABASE_NAME = "LoginDatabase"
        internal const val TABLE_DATA = "users"
        internal const val KEY_NAME = "name"
        internal const val KEY_UNAME = "username"
        internal const val KEY_EMAIL = "email"
        internal const val KEY_PSWD = "pswd"
        internal const val KEY_LANGUAGE = "language"
        internal const val KEY_REGION = "region"
        internal const val KEY_MIN_MOVIE = "minMovie"
        internal const val KEY_MAX_MOVIE = "maxMovie"
        internal const val KEY_MIN_TV = "minTV"
        internal const val KEY_MAX_TV = "maxTV"
        internal const val KEY_OLDEST_DATE = "oldestDate"
        internal const val KEY_RECENT_DATE = "recentDate"
        internal const val KEY_SUBSCRIPTIONS = "subscriptions"
        internal const val KEY_GENRES = "genres"
        internal const val KEY_AVOID_GENRES = "avoidGenres"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE " + TABLE_DATA + " ("
                + KEY_NAME + " TEXT, "
                + KEY_UNAME + " TEXT, "
                + KEY_EMAIL + " TEXT, "
                + KEY_PSWD + " TEXT, "
                + KEY_LANGUAGE + " TEXT, "
                + KEY_REGION + " TEXT, "
                + KEY_MIN_MOVIE + " TEXT, "
                + KEY_MAX_MOVIE + " TEXT, "
                + KEY_MIN_TV + " TEXT, "
                + KEY_MAX_TV + " TEXT, "
                + KEY_OLDEST_DATE + " TEXT, "
                + KEY_RECENT_DATE + " TEXT, "
                + KEY_SUBSCRIPTIONS + " TEXT, "
                + KEY_GENRES + " TEXT, "
                + KEY_AVOID_GENRES + " TEXT"
                + ")")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_DATA")
        onCreate(db)
    }

    fun updateGenresList(username: String, genresList: MutableList<String>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_GENRES, genresList.joinToString(","))
        }
        val success = db.update(TABLE_DATA, values, "$KEY_UNAME=?", arrayOf(username))
        db.close()
        return success != -1
    }

    fun updateServicesList(username: String, servicesList: MutableList<String>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_SUBSCRIPTIONS, servicesList.joinToString(","))
        }
        val success = db.update(TABLE_DATA, values, "$KEY_UNAME=?", arrayOf(username))
        db.close()
        return success != -1
    }


}
