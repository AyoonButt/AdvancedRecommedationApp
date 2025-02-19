package com.example.firedatabase_assis.explore

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.search.PersonDetailFragment
import com.example.firedatabase_assis.search.PosterFragment
import com.example.firedatabase_assis.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoadVideos : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private var offset = 0
    private val limit = 10
    private var isLoading = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_videos)
        setupBottomNavigation(R.id.bottom_menu_explore)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        userViewModel = UserViewModel.getInstance(application)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        adapter = VideoAdapter(
            mutableListOf(),
            this,
            userViewModel.getUser(),
            searchViewModel,
            userViewModel
        )
        recyclerView.adapter = adapter

        val snapHelper = FastSnapHelper(this)
        snapHelper.attachToRecyclerView(recyclerView)


        loadMoviesAndSeriesFromBottomUp()

        searchViewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is SearchViewModel.NavigationState.ShowPoster -> {
                    findViewById<View>(R.id.poster_fragment_container).visibility = View.VISIBLE
                    showPosterFragment(event.id, event.isMovie)
                }

                is SearchViewModel.NavigationState.ShowPerson -> {
                    findViewById<View>(R.id.poster_fragment_container).visibility = View.VISIBLE
                    showPersonFragment(event.id)
                }

                is SearchViewModel.NavigationState.Back -> {
                    if (supportFragmentManager.backStackEntryCount <= 1) {
                        findViewById<View>(R.id.poster_fragment_container).visibility = View.GONE
                    }
                    supportFragmentManager.popBackStack()
                }

                is SearchViewModel.NavigationState.Close -> {
                    findViewById<View>(R.id.poster_fragment_container).visibility = View.GONE
                    supportFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                }
            }
        }


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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

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

    private suspend fun fetchVideosFromApi(limit: Int, offset: Int): List<PostDto> {
        return try {
            val response = postsApi.getPosts(limit, offset)
            Log.d("API_RESPONSE", "Response body: ${response.body()}")

            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("API_ERROR", "Error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("API_EXCEPTION", "Exception: ${e.message}")
            emptyList()
        }
    }

    private fun processMovieVideos(videos: List<PostDto>) {
        try {
            if (videos.isEmpty()) {
                Log.w("PROCESS_WARNING", "No videos to process")
                return
            }

            adapter.addItems(videos)

            videos.forEach { video ->
                Log.d(
                    "VideoKeys",
                    "Video - Post ID: ${video.postId}, Key: ${video.videoKey}, TMDB ID: ${video.tmdbId}, Type: ${video.type}"
                )
            }
        } catch (e: Exception) {
            Log.e("PROCESS_ERROR", "Error processing videos", e)
        }
    }

    private fun showPosterFragment(id: Int, isMovie: Boolean) {
        val posterContainer = findViewById<View>(R.id.poster_fragment_container)
        if (posterContainer?.visibility != View.VISIBLE) {
            posterContainer?.visibility = View.VISIBLE
        }

        val tag = if (isMovie) "poster_movie_$id" else "poster_tv_$id"
        supportFragmentManager.beginTransaction()
            .replace(R.id.poster_fragment_container, PosterFragment.newInstance(id, isMovie), tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun showPersonFragment(id: Int) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.poster_fragment_container,
                PersonDetailFragment.newInstance(id),
                "person_$id"
            )
            .addToBackStack("person_$id")
            .commit()
    }

}
