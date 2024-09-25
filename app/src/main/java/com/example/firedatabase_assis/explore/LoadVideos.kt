package com.example.firedatabase_assis.explore

import FastSnapHelper
import VideoAdapter
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
import com.example.firedatabase_assis.database.Posts
import com.example.firedatabase_assis.home_page.CommentFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class LoadVideos : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private var offset = 0
    private val limit = 10
    private var isLoading = false
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_videos)

        // Fetch userId from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", 0) // Default to 0 if not found

        if (userId == 0) {
            showError("User ID not found in SharedPreferences")
            return
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = VideoAdapter(mutableListOf(), this, userId)
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
                    loadMoviesAndSeriesFromBottomUp()
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
                fetchVideosFromDatabase(limit, offset)
            }
            if (videos.isNotEmpty()) {
                offset += limit
                processMovieVideos(videos) // Pass the entire list of pairs
            }
            isLoading = false
        }
    }

    private fun fetchVideosFromDatabase(limit: Int, offset: Int): List<Pair<String, Int>> {
        val videoData = mutableListOf<Pair<String, Int>>()

        transaction {
            val postsQuery = Posts
                .selectAll()
                .limit(limit, offset.toLong())

            for (post in postsQuery) {
                val postId = post[Posts.postId]
                val videoKey = post[Posts.videoKey]
                if (videoKey != null) {
                    videoData.add(Pair(videoKey, postId))
                }
            }
        }

        return videoData
    }

    private fun processMovieVideos(videos: List<Pair<String, Int>>) {
        adapter.addItems(videos)
        videos.forEach { (videoKey, postId) ->
            Log.d("VideoKeys", "Post ID: $postId, Key: $videoKey")
        }
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
