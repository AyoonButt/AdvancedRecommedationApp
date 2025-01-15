package com.example.firedatabase_assis.explore

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
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
    private lateinit var userViewModel: UserViewModel
    private var offset = 0
    private val limit = 10
    private var isLoading = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_videos)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        userViewModel = UserViewModel.getInstance(application)
        adapter = VideoAdapter(mutableListOf(), this, userViewModel.getUser())
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
            val response = postsApi.getVideos(limit, offset)
            Log.d("API_RESPONSE", "Response body: ${response.body()}")

            if (response.isSuccessful) {
                // Convert VideoResponse objects to Pairs
                response.body()?.mapNotNull { videoPair ->
                    if (videoPair.videoKey != null && videoPair.postId != null) {
                        Pair(videoPair.videoKey, videoPair.postId)
                    } else {
                        Log.w("API_RESPONSE", "Skipping invalid video response: $videoPair")
                        null
                    }
                } ?: emptyList()
            } else {
                Log.e("API_ERROR", "Error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("API_EXCEPTION", "Exception: ${e.message}")
            emptyList()
        }
    }

    private fun processMovieVideos(videos: List<Pair<String, Int>>) {
        try {
            // Skip any pairs where either value is null
            val validVideos = videos.filter { (videoKey, postId) ->
                if (videoKey == null || postId == null) {
                    Log.w("PROCESS_WARNING", "Skipping null pair: ($videoKey, $postId)")
                    false
                } else {
                    true
                }
            }

            if (validVideos.isEmpty()) {
                Log.w("PROCESS_WARNING", "No valid videos after filtering")
                return
            }

            adapter.addItems(validVideos)

            validVideos.forEach { (videoKey, postId) ->
                Log.d("VideoKeys", "Valid entry - Post ID: $postId, Key: $videoKey")
            }
        } catch (e: Exception) {
            Log.e("PROCESS_ERROR", "Error processing videos", e)
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
