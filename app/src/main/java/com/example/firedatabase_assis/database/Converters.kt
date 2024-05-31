package com.example.firedatabase_assis.database


import androidx.room.TypeConverter
import com.example.firedatabase_assis.home_page.Comment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromList(list: List<String>?): String {
        return Gson().toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromCommentList(comments: List<Comment>?): String? {
        if (comments == null) return null
        val gson = Gson()
        val type = object : TypeToken<List<Comment>>() {}.type
        return gson.toJson(comments, type)
    }

    @TypeConverter
    fun toCommentList(commentsString: String?): List<Comment>? {
        if (commentsString == null) return null
        val gson = Gson()
        val type = object : TypeToken<List<Comment>>() {}.type
        return gson.fromJson(commentsString, type)
    }
}
