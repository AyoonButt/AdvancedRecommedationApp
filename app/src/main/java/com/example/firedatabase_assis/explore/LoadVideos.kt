package com.example.firedatabase_assis.explore

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
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
import com.example.firedatabase_assis.postgres.Recommendations
import com.example.firedatabase_assis.search.PersonDetailFragment
import com.example.firedatabase_assis.search.PosterFragment
import com.example.firedatabase_assis.search.SearchViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoadVideos : BaseActivity() {
    private lateinit var binding: ActivityLoadVideosBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recommendations: Recommendations

    // Create the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private lateinit var posts: Posts

    private var offset = 0
    private val limit = 10
    private var isLoading = false
    private var hasMoreData = true
    private var isReturningFromActivity = false

    private var lastFetchTimestamp = 0L
    private var useTimestampBasedFetching = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dark theme before calling super.onCreate
        setTheme(R.style.Theme_VideoPlayer)

        super.onCreate(savedInstanceState)
        binding = ActivityLoadVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up bottom navigation with dark theme
        setupBottomNavigation(R.id.bottom_menu_explore)
        applyDarkNavigationBar()

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshVideoData()
        }

        // Set colors for dark theme refresh animation
        swipeRefreshLayout.setColorSchemeColors(
            resources.getColor(R.color.dark_item_selected, null)
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            resources.getColor(R.color.dark_surface, null)
        )

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
        recommendations = retrofit.create(Recommendations::class.java)
        posts = retrofit.create(Posts::class.java)

        // Initial data load
        loadTrailers()

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
                val threshold = 2 // Slightly smaller threshold for videos since they're larger
                if (!isLoading && hasMoreData &&
                    (visibleItemCount + firstVisibleItemPosition + threshold >= totalItemCount) &&
                    totalItemCount > 0 && // Ensure we have some items
                    firstVisibleItemPosition >= 0
                ) {
                    Log.d("LoadVideos", "Near end of list, loading more trailers...")
                    loadTrailers()
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

    // Apply dark navigation bar styling
    @RequiresApi(Build.VERSION_CODES.M)
    private fun applyDarkNavigationBar() {
        // Apply dark navigation theme programmatically
        baseBinding.bottomNavBar.let { navBar ->
            navBar.setBackgroundColor(resources.getColor(R.color.dark_nav_background, null))
            navBar.itemIconTintList =
                resources.getColorStateList(R.color.dark_bottom_nav_colors, null)
            navBar.itemTextColor = resources.getColorStateList(R.color.dark_bottom_nav_colors, null)
            // Set navigation bar color to match the dark theme
            window.navigationBarColor = resources.getColor(R.color.dark_nav_background, null)
            window.statusBarColor = resources.getColor(R.color.dark_nav_background, null)
        }
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

        // Reset pagination variables
        offset = 0
        lastFetchTimestamp = 0L
        hasMoreData = true

        lifecycleScope.launch {
            try {
                val userId = userViewModel.currentUser.value?.userId ?: return@launch

                // Check post count first to determine which API to use
                val language = userViewModel.currentUser.value?.language ?: "en"
                val countResponse = posts.getPostCountByLanguage(language)

                if (countResponse.isSuccessful) {
                    val count = countResponse.body()?.get("count") ?: 0

                    val response = if (count < 200) {
                        // Use timestamp-based fetching for small datasets
                        useTimestampBasedFetching = true
                        posts.getPostsAfterTimestamp(0) // Start from beginning
                    } else {
                        // Use recommendations for larger datasets
                        useTimestampBasedFetching = false
                        recommendations.getRecommendations(userId, "trailers", limit, 0)
                    }

                    if (response.isSuccessful) {
                        val allVideos = response.body() ?: emptyList()
                        val videos = if (useTimestampBasedFetching) {
                            // Filter for videos only
                            allVideos.filter { it.videoKey.isNotEmpty() }
                        } else {
                            allVideos
                        }

                        // Update timestamp if using timestamp-based fetching
                        if (useTimestampBasedFetching && videos.isNotEmpty()) {
                            val lastVideo = videos.last()
                            val newTimestamp =
                                System.currentTimeMillis() // Assuming posts have a timestamp field
                            lastFetchTimestamp = newTimestamp
                        }

                        if (videos.isEmpty()) {
                            // No content available, show a message
                            hasMoreData = false
                            showError("No trailers available")
                        } else {
                            // Clear existing videos and add new ones
                            adapter.clearAndAddItems(videos)

                            // Scroll to top
                            recyclerView.scrollToPosition(0)

                            // Update offset for next page if using recommendations
                            if (!useTimestampBasedFetching) {
                                offset = videos.size
                            }

                            // Check if there might be more data
                            hasMoreData = videos.size >= limit
                        }
                    } else {
                        handleApiError(response.code(), response.message())
                    }
                } else {
                    // Fall back to recommendations
                    useTimestampBasedFetching = false
                    val response = recommendations.getRecommendations(userId, "trailers", limit, 0)

                    if (response.isSuccessful) {
                        val videos = response.body() ?: emptyList()

                        if (videos.isEmpty()) {
                            // No content available, show a message
                            hasMoreData = false
                            showError("No trailers available")
                        } else {
                            // Clear existing videos and add new ones
                            adapter.clearAndAddItems(videos)

                            // Scroll to top
                            recyclerView.scrollToPosition(0)

                            // Update offset for next page
                            offset = videos.size

                            // Check if there might be more data
                            hasMoreData = videos.size >= limit
                        }
                    } else {
                        handleApiError(response.code(), response.message())
                    }
                }
            } catch (e: Exception) {
                Log.e("LoadVideos", "Exception during refresh", e)
                showError("Error refreshing: ${e.message}")
            } finally {
                // Hide refresh indicator
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // Replace loadTrailers with this implementation
    private fun loadTrailers() {
        if (isLoading || !hasMoreData) return

        isLoading = true

        lifecycleScope.launch {
            try {
                val userId = userViewModel.currentUser.value?.userId ?: return@launch

                // First, check the count of posts with videos in the user's language
                val language = userViewModel.currentUser.value?.language ?: "en"
                val countResponse = posts.getPostCountByLanguage(language)

                if (countResponse.isSuccessful) {
                    val count = countResponse.body()?.get("count") ?: 0

                    if (count < 200) {
                        // Use timestamp-based fetching for small datasets
                        useTimestampBasedFetching = true

                        // Get videos after the last timestamp we've seen
                        val response = posts.getPostsAfterTimestamp(lastFetchTimestamp)

                        if (response.isSuccessful) {
                            val newVideos = response.body()?.filter {
                                // Only include posts with video keys
                                it.videoKey.isNotEmpty()
                            } ?: emptyList()

                            if (newVideos.isEmpty()) {
                                hasMoreData = false
                                Log.d("LoadVideos", "No more trailers available")
                            } else {
                                // Update lastFetchTimestamp to the oldest timestamp in the batch
                                if (newVideos.isNotEmpty()) {
                                    // Update timestamp based on the batch size or what the server returned
                                    val lastVideo = newVideos.last()
                                    val newTimestamp =
                                        System.currentTimeMillis() // Assuming posts have a timestamp field
                                    lastFetchTimestamp = newTimestamp
                                }

                                processVideoResults(newVideos)
                            }
                        } else {
                            handleApiError(response.code(), response.message())
                        }
                    } else {
                        // Use recommendations for larger datasets
                        useTimestampBasedFetching = false

                        // Make the API call with the current offset using recommendations
                        val response =
                            recommendations.getRecommendations(userId, "trailers", limit, offset)

                        if (response.isSuccessful) {
                            val newVideos = response.body() ?: emptyList()
                            processVideoResults(newVideos)
                        } else {
                            handleApiError(response.code(), response.message())
                        }
                    }
                } else {
                    // Fall back to recommendations if count check fails
                    useTimestampBasedFetching = false
                    val response =
                        recommendations.getRecommendations(userId, "trailers", limit, offset)

                    if (response.isSuccessful) {
                        val newVideos = response.body() ?: emptyList()
                        processVideoResults(newVideos)
                    } else {
                        handleApiError(response.code(), response.message())
                    }
                }
            } catch (e: Exception) {
                Log.e("LoadVideos", "Error loading trailers", e)
                showError("Failed to load trailers: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun processVideoResults(newVideos: List<PostDto>) {
        if (newVideos.isEmpty()) {
            hasMoreData = false
            Log.d("LoadVideos", "No more trailers available")
            return
        }

        // Filter out videos we already have to avoid duplicates
        val existingIds = adapter.getItems().map { it.postId }.toSet()
        val uniqueNewVideos = newVideos.filter { it.postId !in existingIds }

        if (uniqueNewVideos.isEmpty()) {
            // All videos were duplicates, no more new data
            hasMoreData = false
            return
        }

        // Log loading information
        Log.d(
            "LoadVideos",
            "Loaded ${uniqueNewVideos.size} trailers, new offset: ${offset + uniqueNewVideos.size}"
        )

        // Update offset for next page when using recommendations
        if (!useTimestampBasedFetching) {
            offset += uniqueNewVideos.size
        }

        // Process videos and add to adapter
        processMovieVideos(uniqueNewVideos)

        // Update hasMoreData flag
        hasMoreData = uniqueNewVideos.size >= limit / 2 // Allow for some filtering
    }

    // Helper function to handle API errors
    private fun handleApiError(code: Int, message: String) {
        Log.e("LoadVideos", "Error: $code - $message")
        showError("Failed to load trailers: $message")
    }


    private suspend fun fetchVideosFromApi(limit: Int, offset: Int): List<PostDto> {
        return try {
            val userId = userViewModel.currentUser.value?.userId ?: return emptyList()

            val response = recommendations.getRecommendations(userId, "trailers", limit, offset)

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