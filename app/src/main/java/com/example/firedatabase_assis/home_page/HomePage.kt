package com.example.firedatabase_assis.home_page

import android.content.Intent
import android.os.Bundle
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
import com.example.firedatabase_assis.databinding.ActivityHomePageBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.Recommendations
import com.example.firedatabase_assis.search.NavigationManager
import com.example.firedatabase_assis.search.PersonDetailFragment
import com.example.firedatabase_assis.search.PosterFragment
import com.example.firedatabase_assis.search.SearchViewModel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomePage : BaseActivity() {
    private lateinit var binding: ActivityHomePageBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyPostAdapter
    private lateinit var recommendations: Recommendations
    private lateinit var posts: Posts
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var navigationManager: NavigationManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    private var postData: MutableList<PostDto> = mutableListOf()
    private var isLoading = false

    private var isReturningFromActivity = false
    private var offset = 0
    private val limit = 20
    private var hasMoreData = true  // Add this flag to track when there's no more data
    private var lastFetchTimestamp = 0L
    private var useTimestampBasedFetching = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_home)

        swipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }


        userViewModel = UserViewModel.getInstance(application)
        navigationManager = NavigationManager(supportFragmentManager)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyPostAdapter(this, movies = postData, userViewModel, searchViewModel)
        recyclerView.adapter = adapter

        // Initialize Retrofit
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        recommendations = retrofit.create(Recommendations::class.java)
        posts = retrofit.create(Posts::class.java)


        handleInitialLoadOrRefresh()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy <= 0) return  // Only load more when scrolling down

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Check if we need to load more data
                // Load when user has scrolled to the last 3 items
                val threshold = 3
                if (!isLoading && hasMoreData &&
                    (visibleItemCount + firstVisibleItemPosition + threshold >= totalItemCount) &&
                    firstVisibleItemPosition >= 0
                ) {
                    Log.d("HomePage", "Near end of list, loading more data...")
                    loadData()
                }
            }
        })

        searchViewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is SearchViewModel.NavigationState.ShowPoster -> {
                    findViewById<View>(R.id.poster_container).visibility = View.VISIBLE
                    showPosterFragment(event.id, event.isMovie)
                }

                is SearchViewModel.NavigationState.ShowPerson -> {
                    findViewById<View>(R.id.poster_container).visibility = View.VISIBLE
                    showPersonFragment(event.id)
                }

                is SearchViewModel.NavigationState.Back -> {
                    if (navigationManager.isRootFragment()) {
                        findViewById<View>(R.id.poster_container).visibility = View.GONE
                    }
                    supportFragmentManager.popBackStack()
                }

                is SearchViewModel.NavigationState.Close -> {
                    findViewById<View>(R.id.poster_container).visibility = View.GONE
                    supportFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                }

                else -> {}
            }
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Check if we're coming from another tab (FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (intent?.flags?.and(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) {
            // Check if this is an explicit navigation request or just a return
            val isExplicitNavigation = intent?.getBooleanExtra("is_navigation", false)

            if (isExplicitNavigation == false) {
                refreshData()
            } else {
                // Just returning to a previously opened tab, don't refresh
            }
        }
    }

    private var hasInitiallyLoaded = false

    private fun handleInitialLoadOrRefresh() {
        if (!hasInitiallyLoaded) {
            // First time this activity is created
            loadData() // Initial load
            hasInitiallyLoaded = true
        } else if (intent?.getBooleanExtra("is_navigation", false) == true) {
            // Explicit navigation from tab bar
            refreshData()
        }
        // If neither condition is met, do nothing - we're just returning from another activity
    }

    fun refreshData() {
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

                    val response = if (count < 50) {
                        // Use timestamp-based fetching for small datasets
                        useTimestampBasedFetching = true
                        posts.getPostsAfterTimestamp(0) // Start from beginning
                    } else {
                        // Use recommendations for larger datasets
                        useTimestampBasedFetching = false
                        recommendations.getRecommendations(userId, "posts", limit, 0)
                    }

                    if (response.isSuccessful) {
                        val newPosts = response.body() ?: emptyList()

                        // Update timestamp if using timestamp-based fetching
                        if (useTimestampBasedFetching && newPosts.isNotEmpty()) {
                            val lastPost = newPosts.last()
                            val newTimestamp =
                                System.currentTimeMillis() // Assuming posts have a timestamp field
                            lastFetchTimestamp = newTimestamp
                        }

                        // Clear existing data and add new data
                        postData.clear()
                        postData.addAll(newPosts)
                        adapter.notifyDataSetChanged()

                        // Update offset for next page if using recommendations
                        if (!useTimestampBasedFetching) {
                            offset = newPosts.size
                        }

                        // Check if there might be more data
                        hasMoreData = newPosts.size >= limit

                        // Scroll to top
                        recyclerView.scrollToPosition(0)
                    } else {
                        handleApiError(response.code(), response.message())
                    }
                } else {
                    // Fall back to recommendations
                    useTimestampBasedFetching = false
                    val response = recommendations.getRecommendations(userId, "posts", limit, 0)

                    if (response.isSuccessful) {
                        handleRefreshResponse(response)
                    } else {
                        handleApiError(response.code(), response.message())
                    }
                }
            } catch (e: Exception) {
                Log.e("HomePage", "Exception during refresh", e)
                showError("Error refreshing: ${e.message}")
            } finally {
                // Hide refresh indicator
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // Helper function to handle refresh response
    private fun handleRefreshResponse(response: retrofit2.Response<List<PostDto>>) {
        val newPosts = response.body() ?: emptyList()

        // Clear existing data and add new data
        postData.clear()
        postData.addAll(newPosts)
        adapter.notifyDataSetChanged()

        // Update offset for next page if using recommendations
        if (!useTimestampBasedFetching) {
            offset = newPosts.size
        }

        // Check if there might be more data
        hasMoreData = newPosts.size >= limit

        // Scroll to top
        recyclerView.scrollToPosition(0)
    }

    private fun loadData() {
        if (isLoading || !hasMoreData) return  // Don't load if already loading or no more data

        isLoading = true

        lifecycleScope.launch {
            try {
                val userId = userViewModel.currentUser.value?.userId ?: return@launch

                // First, check the count of posts in the user's language
                val language = userViewModel.currentUser.value?.language ?: "en"
                val countResponse = posts.getPostCountByLanguage(language)

                if (countResponse.isSuccessful) {
                    val count = countResponse.body()?.get("count") ?: 0

                    // Decide which API to use based on the count
                    if (count < 50) {
                        // Use timestamp-based fetching for small datasets
                        useTimestampBasedFetching = true

                        // Get posts after the last timestamp we've seen
                        val response = posts.getPostsAfterTimestamp(lastFetchTimestamp)

                        if (response.isSuccessful) {
                            val newPosts = response.body() ?: emptyList()

                            if (newPosts.isEmpty()) {
                                hasMoreData = false
                                if (postData.isEmpty()) {
                                    showError("No posts available")
                                }
                            } else {
                                // Update lastFetchTimestamp to the oldest timestamp in the batch
                                // This assumes the API returns posts in descending order by timestamp
                                if (newPosts.isNotEmpty()) {
                                    // Update timestamp based on the batch size or what the server returned
                                    val lastPost = newPosts.last()
                                    val newTimestamp =
                                        System.currentTimeMillis() // Assuming posts have a timestamp field
                                    lastFetchTimestamp = newTimestamp
                                }

                                processFetchedPosts(newPosts)
                            }
                        } else {
                            handleApiError(response.code(), response.message())
                        }
                    } else {
                        // Use recommendations for larger datasets
                        useTimestampBasedFetching = false

                        // Make the API call with the current offset using recommendations
                        val response =
                            recommendations.getRecommendations(userId, "posts", limit, offset)

                        if (response.isSuccessful) {
                            val newPosts = response.body() ?: emptyList()
                            processFetchedPosts(newPosts)
                        } else {
                            handleApiError(response.code(), response.message())
                        }
                    }
                } else {
                    // Fall back to recommendations if count check fails
                    useTimestampBasedFetching = false
                    val response =
                        recommendations.getRecommendations(userId, "posts", limit, offset)

                    if (response.isSuccessful) {
                        val newPosts = response.body() ?: emptyList()
                        processFetchedPosts(newPosts)
                    } else {
                        handleApiError(response.code(), response.message())
                    }
                }
            } catch (e: Exception) {
                Log.e("HomePage", "Exception during data loading", e)
                e.printStackTrace()
                showError("Error loading data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun handleApiError(code: Int, message: String) {
        Log.e("HomePage", "Error: $code - $message")
        showError("Failed to load data: $message")
    }

    private fun processFetchedPosts(newPosts: List<PostDto>) {
        if (newPosts.isEmpty()) {
            // No more data available
            hasMoreData = false
            if (postData.isEmpty()) {
                showError("No posts available")
            }
        } else {
            // Filter out posts we already have to avoid duplicates
            val existingIds = postData.map { it.postId }.toSet()
            val uniqueNewPosts = newPosts.filter { it.postId !in existingIds }

            if (uniqueNewPosts.isEmpty()) {
                // All posts were duplicates, no more new data
                hasMoreData = false
                return
            }

            // Log fetched posts
            Log.d("HomePage", "Loaded ${uniqueNewPosts.size} more posts")

            // Update the data
            val startPosition = postData.size
            postData.addAll(uniqueNewPosts)

            // Better notification - only update the new items
            if (startPosition == 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyItemRangeInserted(startPosition, uniqueNewPosts.size)
            }

            // Update offset for next page when using recommendations
            if (!useTimestampBasedFetching) {
                offset += uniqueNewPosts.size
            }

            // Determine if there might be more data
            hasMoreData = uniqueNewPosts.size >= limit / 2 // Allow for some filtering
        }
    }

    private fun showError(message: String) {
        // Show error message to user
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }


    private fun showPosterFragment(id: Int, isMovie: Boolean) {
        val tag = if (isMovie) "poster_movie_$id" else "poster_tv_$id"
        binding.posterContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.poster_container, PosterFragment.newInstance(id, isMovie), tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun showPersonFragment(id: Int) {
        binding.posterContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.poster_container, PersonDetailFragment.newInstance(id), "person_$id")
            .addToBackStack("person_$id")
            .commit()
    }

    override fun onPause() {
        super.onPause()
        (recyclerView.adapter as? MyPostAdapter)?.saveAllVisibleInteractions(recyclerView)
    }

    override fun onStop() {
        super.onStop()
        (recyclerView.adapter as? MyPostAdapter)?.saveAllVisibleInteractions(recyclerView)
    }

}

