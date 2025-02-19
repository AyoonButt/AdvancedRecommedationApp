package com.example.firedatabase_assis.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import kotlinx.coroutines.launch

class SearchActivity : BaseActivity() {
    private lateinit var mediaAdapter: MediaItemAdapter
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var recyclerViewMovies: RecyclerView
    private lateinit var recyclerViewProfiles: RecyclerView
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSearch: Button
    private lateinit var viewModel: SearchViewModel
    private lateinit var navigationManager: NavigationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setupBottomNavigation(R.id.bottom_menu_search)

        navigationManager = NavigationManager(supportFragmentManager)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        // Check if this is a refresh
        if (intent.getBooleanExtra("refresh", false)) {
            viewModel.clearLists()  // Clear existing data
        }

        initializeViews()
        setupAdapters()
        setupRecyclerViews()
        setupObservers()
        setupSearch()
        setupPagination()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun initializeViews() {
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearch = findViewById(R.id.buttonSearch)
        recyclerViewProfiles = findViewById(R.id.recyclerViewProfiles)
        recyclerViewMovies = findViewById(R.id.recyclerViewMovies)
    }

    private fun setupAdapters() {
        // Movie Adapter with click listener
        mediaAdapter = MediaItemAdapter(
            onItemClick = { item ->
                if (!viewModel.isLoading.value) {
                    viewModel.setSelectedItem(item) // Set the selected item in the view model

                    // Check if the item is a Movie or TV and pass the isMovie flag
                    val isMovie = item is Movie

                    // Show PosterFragment with the corresponding flag
                    showFragment(PosterFragment.newInstance(item.id, isMovie))
                }
            },
            isRecommendation = false
        )


        // Profile Adapter with click listener
        profileAdapter = ProfileAdapter { person ->
            showFragment(PersonDetailFragment.newInstance(person.id))
        }
    }

    private fun setupRecyclerViews() {
        // Profiles RecyclerView
        recyclerViewProfiles.apply {
            layoutManager = LinearLayoutManager(
                this@SearchActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = profileAdapter
        }

        // Media RecyclerView
        recyclerViewMovies.apply {
            layoutManager = GridLayoutManager(this@SearchActivity, 2)
            adapter = mediaAdapter
        }
    }

    private fun setupObservers() {
        viewModel.navigationEvent.observe(this) { event ->
            navigationManager.handleNavigation(event)
            findViewById<View>(R.id.container).visibility = when {
                event is SearchViewModel.NavigationState.Close -> View.GONE
                event is SearchViewModel.NavigationState.Back && navigationManager.isRootFragment() -> View.GONE
                else -> View.VISIBLE
            }
        }

        viewModel.profiles.observe(this) { profiles ->
            println("Received profiles: ${profiles.size}")  // Debug log
            val validProfiles = profiles.filter { !it.profile_path.isNullOrEmpty() }
            println("Valid profiles: ${validProfiles.size}")  // Debug log
            recyclerViewProfiles.visibility = if (validProfiles.isEmpty()) View.GONE else {
                println("Setting profiles visible")  // Debug log
                View.VISIBLE
            }
            profileAdapter.submitList(validProfiles)
        }


        viewModel.mediaItems.observe(this) { mediaItems ->
            mediaAdapter.submitList(mediaItems)
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                buttonSearch.isEnabled = !isLoading
            }
        }
    }

    private fun setupSearch() {
        editTextSearch.doAfterTextChanged { text ->
            text?.toString()?.takeIf { it.isNotBlank() }?.let(viewModel::onSearchInput)
        }

        buttonSearch.setOnClickListener {
            editTextSearch.text?.toString()?.takeIf { it.isNotBlank() }?.let { query ->
                hideKeyboard()
                viewModel.onSearchInput(query)
            }
        }
    }

    private fun setupPagination() {
        recyclerViewMovies.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return // Skip if scrolling up

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                if (!viewModel.isLoading.value && lastVisibleItem >= totalItemCount - 5) {
                    viewModel.loadNextPage()
                }
            }
        })

        // Observe paging state
        lifecycleScope.launch {
            viewModel.pagingState.collect { state ->
                when (state) {
                    is SearchViewModel.PagingState.Error -> {
                        // Show error state (e.g., Snackbar)
                    }

                    else -> {} // Handle other states if needed
                }
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        val fragmentTag = when (fragment) {
            is PosterFragment -> {
                val args = fragment.arguments
                when {
                    // Check if the item is a Movie or TV using the 'isMovie' flag
                    args?.containsKey("is_movie") == true -> {
                        val isMovie = args.getBoolean("is_movie")
                        if (isMovie) {
                            // Create a tag for Movie
                            "poster_movie_${args.getInt("item_id")}"
                        } else {
                            // Create a tag for TV
                            "poster_tv_${args.getInt("item_id")}"
                        }
                    }

                    else -> "poster_unknown"
                }
            }

            is PersonDetailFragment -> {
                "person_${fragment.arguments?.getInt("person_id")}"
            }

            else -> fragment.javaClass.simpleName
        }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.container, fragment, fragmentTag)
            .hide(supportFragmentManager.fragments.lastOrNull() ?: return)
            .addToBackStack(fragmentTag)
            .commit()

        findViewById<View>(R.id.container).visibility = View.VISIBLE
    }


    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(buttonSearch.windowToken, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val container = findViewById<View>(R.id.container)
        if (container?.visibility == View.VISIBLE) {
            viewModel.navigate(SearchViewModel.NavigationState.Back)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseSearch()
    }

    override fun onResume() {
        super.onResume()
        // Only resume search if there's text in the search box
        editTextSearch.text?.toString()?.takeIf { it.isNotBlank() }?.let { query ->
            viewModel.resumeSearch()
        }
    }

    override fun onDestroy() {
        try {
            if (isFinishing) {
                recyclerViewMovies.layoutManager = null
                recyclerViewProfiles.layoutManager = null

                mediaAdapter.cleanup()
                profileAdapter.cleanup()

                // Clear scroll listeners before adapters
                recyclerViewMovies.clearOnScrollListeners()

                // Set adapters to null last
                recyclerViewMovies.adapter = null
                recyclerViewProfiles.adapter = null
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        super.onDestroy()
    }

}