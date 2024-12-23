package com.example.firedatabase_assis.search

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class SearchActivity : AppCompatActivity() {
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSearch: Button
    private lateinit var recyclerViewProfiles: RecyclerView
    private lateinit var recyclerViewMovies: RecyclerView

    private val profilesList = mutableListOf<Person>()
    private val moviesList = mutableListOf<Movie>()
    private val TVList = mutableListOf<TV>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearch = findViewById(R.id.buttonSearch)
        recyclerViewProfiles = findViewById(R.id.recyclerViewProfiles)
        recyclerViewMovies = findViewById(R.id.recyclerViewMovies)

        buttonSearch.setOnClickListener {
            val query = editTextSearch.text.toString()
            if (query.isNotEmpty()) {
                fetchData(query)
            }
        }
    }

    private fun fetchData(query: String) {
        val client = OkHttpClient()

        fun fetchPage(page: Int) {
            val request = Request.Builder()
                .url("https://api.themoviedb.org/3/search/multi?query=$query&include_adult=false&language=en-US&page=$page")
                .get()
                .addHeader("accept", "application/json")
                .addHeader(
                    "Authorization",
                    "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}"
                )
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    // Handle error (e.g., show a message to the user)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.let { responseBody ->
                        val json = responseBody.string()
                        Log.d("SearchActivity", "Response JSON: $json")

                        val searchResult = Gson().fromJson(json, JsonObject::class.java)
                        val results = searchResult.getAsJsonArray("results")

                        if (results != null) {
                            results.forEach { resultJson ->
                                val result = Gson().fromJson(resultJson, JsonObject::class.java)
                                val mediaType = result.get("media_type")?.asString

                                when (mediaType) {
                                    "movie" -> {
                                        val movie = Gson().fromJson(result, Movie::class.java)
                                        moviesList.add(movie)
                                    }

                                    "tv" -> {
                                        val tvShow = Gson().fromJson(result, TV::class.java)
                                        TVList.add(tvShow)
                                    }

                                    "person" -> {
                                        val person = Gson().fromJson(result, Person::class.java)
                                        profilesList.add(person)
                                    }
                                }
                            }
                        }

                        val totalPagesElement = searchResult.get("total_pages")
                        val totalPages =
                            if (totalPagesElement != null && !totalPagesElement.isJsonNull) {
                                totalPagesElement.asInt
                            } else {
                                0 // Default value or handle the case where total_pages is missing
                            }
                        Log.d("SearchActivity", "Total Pages: $totalPages")

                        if (page < totalPages) {
                            fetchPage(page + 1)
                        } else {
                            runOnUiThread {
                                updateRecyclerViews()
                            }
                        }
                    } ?: run {
                        // Handle null responseBody
                    }
                }
            })
        }

        profilesList.clear()
        moviesList.clear()
        TVList.clear()
        fetchPage(1)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val fragmentContainer = findViewById<View>(R.id.container)

        // Check if the fragment container is visible
        if (fragmentContainer.visibility == View.VISIBLE) {
            fragmentContainer.visibility = View.GONE
            // Optionally, you can also pop the fragment back stack if needed
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun updateRecyclerViews() {
        val profileAdapter = ProfileAdapter(profilesList)
        recyclerViewProfiles.adapter = profileAdapter
        recyclerViewProfiles.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val movieAdapter = MovieTVAdapter(moviesList, TVList)
        recyclerViewMovies.adapter = movieAdapter
        recyclerViewMovies.layoutManager =
            GridLayoutManager(this, 2)
    }


}
