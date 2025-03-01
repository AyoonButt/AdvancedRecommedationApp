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
    private lateinit var postsService: Posts
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var navigationManager: NavigationManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout


    private var postData: MutableList<PostDto> = mutableListOf()
    private var isLoading = false

    private var isReturningFromActivity = false
    private var offset = 0
    private val limit = 10
    private var hasMoreData = true  // Add this flag to track when there's no more data


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

        postsService = retrofit.create(Posts::class.java)


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
                // Load when user has scrolled to the last 2-3 items
                val threshold = 3
                if (!isLoading && hasMoreData &&
                    (visibleItemCount + firstVisibleItemPosition + threshold >= totalItemCount) &&
                    firstVisibleItemPosition >= 0
                ) {
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

        // Get the current offset (to load newer content)
        val currentOffset = offset

        // We'll load newer content, so keep hasMoreData true
        hasMoreData = true

        // Show loading indicator

        // Load fresh data
        lifecycleScope.launch {
            try {
                // Get a new page of content (different than what we have)
                val newPageOffset = offset + limit
                val response = postsService.getPosts(limit, newPageOffset)

                if (response.isSuccessful) {
                    val newMovies = response.body() ?: emptyList()

                    if (newMovies.isEmpty()) {
                        // No newer content available, show a message
                        hasMoreData = false
                        showError("No new content available")
                    } else {
                        // Insert at the beginning (newer content appears at the top)
                        postData.addAll(0, newMovies)
                        adapter.notifyItemRangeInserted(0, newMovies.size)

                        // Scroll to show the new content
                        recyclerView.scrollToPosition(0)

                        // Update offset to include the new items
                        offset += newMovies.size
                    }
                } else {
                    Log.e(
                        "HomePage",
                        "Error refreshing: ${response.code()} - ${response.message()}"
                    )
                    showError("Failed to refresh: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("HomePage", "Exception during refresh", e)
                showError("Error refreshing: ${e.message}")
            } finally {
                // Hide both indicators
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadData() {
        if (isLoading || !hasMoreData) return  // Don't load if already loading or no more data

        isLoading = true


        lifecycleScope.launch {
            try {
                // Make the API call
                val response = postsService.getPosts(limit, offset)

                // Check if the response is successful
                if (response.isSuccessful) {
                    val newMovies = response.body() ?: emptyList()

                    if (newMovies.isEmpty()) {
                        // No more data available
                        hasMoreData = false
                    } else {
                        // Log fetched movies
                        newMovies.forEach { movie ->
                            Log.d("HomePage", "Movie: id=${movie.postId}, title=${movie.title}")
                        }

                        // Update the data
                        val startPosition = postData.size
                        postData.addAll(newMovies)

                        // Better notification - only update the new items
                        if (startPosition == 0) {
                            adapter.notifyDataSetChanged()
                        } else {
                            adapter.notifyItemRangeInserted(startPosition, newMovies.size)
                        }

                        // Update offset for next page
                        offset += newMovies.size
                    }
                } else {
                    // Log or handle the error response
                    Log.e("HomePage", "Error: ${response.code()} - ${response.message()}")
                    showError("Failed to load data: ${response.message()}")
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

