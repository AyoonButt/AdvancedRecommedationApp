package com.example.firedatabase_assis.explore

import FastSnapHelper
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.MovieDatabase
import com.example.firedatabase_assis.home_page.CommentFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class LoadVideos : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private val client = OkHttpClient()
    private var offset = 0
    private val limit = 10
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_videos)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = VideoAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        val snapHelper = FastSnapHelper(this)
        snapHelper.attachToRecyclerView(recyclerView)

        loadMoviesFromBottomUp()



        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    loadMoviesFromBottomUp()
                }
            }
        })

        recyclerView.addOnChildAttachStateChangeListener(object :
            RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                val holder = recyclerView.getChildViewHolder(view) as VideoAdapter.ViewHolder
                holder.youTubePlayer?.play()
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                val holder = recyclerView.getChildViewHolder(view) as VideoAdapter.ViewHolder
                holder.youTubePlayer?.pause()
            }
        })
    }

    private fun loadMoviesFromBottomUp() {
        isLoading = true
        val database = MovieDatabase.getDatabase(applicationContext)
        val primeDao = database.primeMovieDao()

        lifecycleScope.launch {
            val movies = withContext(Dispatchers.IO) {
                primeDao.getMoviesWithPagination(limit, offset)
            }
            if (movies.isNotEmpty()) {
                offset += limit
                for (movie in movies) {
                    fetchMovieVideos(movie.id)
                }
            }
            isLoading = false
        }
    }

    private fun fetchMovieVideos(movieId: Int) {
        val url = "https://api.themoviedb.org/3/movie/$movieId/videos?language=en-US"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json")
            .addHeader(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOWRkOWQzYWU4MzhkYjE4ZDUxZjg4Y2Q1MGU0NzllNCIsIm5iZiI6MTcyMTA4Mzk5MS42ODc5NTUsInN1YiI6IjY2MjZiM2ZkMjU4ODIzMDE2NDkxODliMSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.dXcgTC2h_FTmM94Xx-pE04jF3F8tPoFYBxcKMmnV338"
            )
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    val jsonObject = JSONObject(jsonData)
                    val results = jsonObject.getJSONArray("results")

                    withContext(Dispatchers.Main) {
                        processMovieVideos(results)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Failed to fetch movie videos")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("An error occurred")
                }
            }
        }
    }

    private fun processMovieVideos(results: JSONArray) {
        val videoKeys = mutableListOf<String>()
        for (i in 0 until results.length()) {
            val videoObject = results.getJSONObject(i)
            if (videoObject.getString("site") == "YouTube") {
                val videoKey = videoObject.getString("key")
                videoKeys.add(videoKey)
                Log.d("VideoKeys", "Keu: $videoKey")
            }
        }
        adapter.addVideos(videoKeys)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Check if CommentFragment is visible
        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        if (fragmentContainer != null && fragmentContainer.visibility == View.VISIBLE) {
            // If CommentFragment is visible, pass the event to it
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment is CommentFragment) {
                val result = fragment.dispatchTouchEvent(event)
                if (result) {
                    return true
                }
            }
        }
        // Otherwise, handle the event in the HomePage activity
        return super.dispatchTouchEvent(event)
    }


}
