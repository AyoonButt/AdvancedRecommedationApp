package com.example.firedatabase_assis.explore

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.home_page.CommentFragment
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Posts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoadVideos : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private var offset = 0
    private val limit = 10
    private var isLoading = false

    private val userViewModel: UserViewModel by viewModels() // Initialize UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_videos)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = VideoAdapter(mutableListOf(), this, userViewModel)
        recyclerView.adapter = adapter

        val snapHelper = FastSnapHelper(this)
        snapHelper.attachToRecyclerView(recyclerView)


        loadMoviesAndSeriesFromBottomUp()


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    userViewModel.currentUser.value?.userId?.let {
                        loadMoviesAndSeriesFromBottomUp()
                    }
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

    private fun loadMoviesAndSeriesFromBottomUp() {
        isLoading = true
        lifecycleScope.launch {
            val videos = withContext(Dispatchers.IO) {
                fetchVideosFromApi(limit, offset)
            }
            if (videos.isNotEmpty()) {
                offset += limit
                processMovieVideos(videos) // Pass the entire list of pairs
            }
            isLoading = false
        }
    }

    // Create the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL) // Replace with your API base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instantiate your Posts API
    private val postsApi = retrofit.create(Posts::class.java)

    private suspend fun fetchVideosFromApi(limit: Int, offset: Int): List<Pair<String, Int>> {
        return try {
            // Make the API call
            val response = postsApi.getVideos(limit, offset)

            // Check if the response is successful
            if (response.isSuccessful) {
                response.body() ?: emptyList() // Return the list or an empty list if null
            } else {
                // Handle the error
                println("Error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            // Handle exceptions (e.g., network issues)
            println("Exception: ${e.message}")
            emptyList()
        }
    }

    private fun processMovieVideos(videos: List<Pair<String, Int>>) {
        adapter.addItems(videos)
        videos.forEach { (videoKey, postId) ->
            Log.d("VideoKeys", "Post ID: $postId, Key: $videoKey")
        }
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
