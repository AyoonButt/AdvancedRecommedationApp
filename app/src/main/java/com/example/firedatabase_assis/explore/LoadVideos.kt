package com.example.firedatabase_assis.explore

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityLoadVideosBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.search.PersonDetailFragment
import com.example.firedatabase_assis.search.PosterFragment
import com.example.firedatabase_assis.search.SearchViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoadVideos : BaseActivity() {
    private lateinit var binding: ActivityLoadVideosBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var postsService: Posts

    // Create the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instantiate your Posts API
    private lateinit var postsApi: Posts

    private var offset = 0
    private val limit = 10
    private var isLoading = false
    private var hasMoreData = true
    private var isReturningFromActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_explore)

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshVideoData()
        }

        // Set colors for refresh animation

        recyclerView = binding.recyclerView
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

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        postsService = retrofit.create(Posts::class.java)
        postsApi = postsService // Assign to the existing postsApi field

        // Initial data load
        loadMoviesAndSeriesFromBottomUp()

        searchViewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is SearchViewModel.NavigationState.ShowPoster -> {
                    binding.posterFragmentContainer.visibility = View.VISIBLE
                    showPosterFragment(event.id, event.isMovie)
                }

                is SearchViewModel.NavigationState.ShowPerson -> {
                    binding.posterFragmentContainer.visibility = View.VISIBLE
                    showPersonFragment(event.id)
                }

                is SearchViewModel.NavigationState.Back -> {
                    if (supportFragmentManager.backStackEntryCount <= 1) {
                        binding.posterFragmentContainer.visibility = View.GONE
                    }
                    supportFragmentManager.popBackStack()
                }

                is SearchViewModel.NavigationState.Close -> {
                    binding.posterFragmentContainer.visibility = View.GONE
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

                if (dy <= 0) return // Only load more when scrolling down

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Check if we need to load more
                val threshold = 3
                if (!isLoading && hasMoreData &&
                    (visibleItemCount + firstVisibleItemPosition + threshold >= totalItemCount) &&
                    firstVisibleItemPosition >= 0
                ) {
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

    override fun onResume() {
        super.onResume()

        // We're returning from another activity
        isReturningFromActivity = true

        // Reset after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            isReturningFromActivity = false
        }, 500) // Half-second delay
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Check if we're coming from another tab (FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (intent?.flags?.and(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) {
            // Check if this is an explicit navigation request or just a return
            val isExplicitNavigation = intent?.getBooleanExtra("is_navigation", false)

            if (isExplicitNavigation == false) {
                refreshVideoData()
            } else {
                // Just returning to a previously opened tab, don't refresh
            }
        }
    }

    fun refreshVideoData() {
        // Show the refresh indicator
        swipeRefreshLayout.isRefreshing = true

        // Get the current offset (to load newer content)
        val currentOffset = offset

        // We'll load newer content, so keep hasMoreData true
        hasMoreData = true

        // Load fresh data
        lifecycleScope.launch {
            try {
                // Get a new page of content - preferably newer than current
                val newPageOffset = offset + limit
                val videos = withContext(Dispatchers.IO) {
                    fetchVideosFromApi(limit, newPageOffset)
                }

                if (videos.isEmpty()) {
                    // No newer content available, show a message
                    hasMoreData = false
                    showError("No new content available")
                } else {
                    // Insert at the beginning (newer content appears at the top)
                    adapter.addItemsAtBeginning(videos)

                    // Scroll to show the new content
                    recyclerView.scrollToPosition(0)

                    // Update offset to include the new items
                    offset += videos.size
                }
            } catch (e: Exception) {
                Log.e("LoadVideos", "Exception during refresh", e)
                showError("Error refreshing: ${e.message}")
            } finally {
                // Hide indicators
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadMoviesAndSeriesFromBottomUp() {
        if (isLoading || !hasMoreData) return

        isLoading = true

        lifecycleScope.launch {
            try {
                val videos = withContext(Dispatchers.IO) {
                    fetchVideosFromApi(limit, offset)
                }

                if (videos.isEmpty()) {
                    hasMoreData = false
                } else {
                    offset += videos.size
                    processMovieVideos(videos)
                }
            } catch (e: Exception) {
                Log.e("LoadVideos", "Error loading videos", e)
                showError("Failed to load videos: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

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


    private fun showError(message: String) {
        // Show error message to user
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
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
        val posterContainer = binding.posterFragmentContainer
        if (posterContainer.visibility != View.VISIBLE) {
            posterContainer.visibility = View.VISIBLE
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