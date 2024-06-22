package com.example.firedatabase_assis.login_setup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class InfoDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "Providers.db"
        private const val TABLE_PROVIDERS = "providers"
        private const val COLUMN_PROVIDER_NAME = "provider_name"
        private const val COLUMN_PROVIDER_ID = "provider_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE $TABLE_PROVIDERS "
                + "($COLUMN_PROVIDER_NAME TEXT PRIMARY KEY, "
                + "$COLUMN_PROVIDER_ID INTEGER)")
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROVIDERS")
        onCreate(db)
    }

    fun insertProvider(provider: Provider) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROVIDER_NAME, provider.provider_name)
            put(COLUMN_PROVIDER_ID, provider.provider_id)
        }
        db.insert(TABLE_PROVIDERS, null, values)
        db.close()
    }

    fun getAllProviders(): List<Provider> {
        val providers = mutableListOf<Provider>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PROVIDERS", null)

        cursor.use {
            val nameIndex = it.getColumnIndex(COLUMN_PROVIDER_NAME)
            val idIndex = it.getColumnIndex(COLUMN_PROVIDER_ID)

            while (it.moveToNext()) {
                val providerName = if (nameIndex != -1) it.getString(nameIndex) else null
                val providerId =
                    if (idIndex != -1) it.getInt(idIndex) else -1 // Handle default value appropriately

                if (providerName != null && providerId != -1) {
                    providers.add(Provider(providerName, providerId))
                }
            }
        }


        db.close()
        return providers
    }
}
