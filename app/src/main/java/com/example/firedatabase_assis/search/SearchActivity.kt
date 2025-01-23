package com.example.firedatabase_assis.search

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private lateinit var movieAdapter: MovieTVAdapter
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

        navigationManager = NavigationManager(supportFragmentManager)
        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        initializeViews()
        setupAdapters()
        setupRecyclerViews()
        setupObservers()
        setupSearch()
    }

    private fun initializeViews() {
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearch = findViewById(R.id.buttonSearch)
        recyclerViewProfiles = findViewById(R.id.recyclerViewProfiles)
        recyclerViewMovies = findViewById(R.id.recyclerViewMovies)
    }

    private fun setupAdapters() {
        // Movie Adapter with click listener
        movieAdapter = MovieTVAdapter { item ->
            if (!viewModel.isLoading.value) {
                viewModel.setSelectedItem(item)
                showFragment(PosterFragment.newInstance(item))
            }
        }

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

        // Movies RecyclerView
        recyclerViewMovies.apply {
            layoutManager = GridLayoutManager(this@SearchActivity, 2)
            adapter = movieAdapter
        }
    }

    private fun setupObservers() {

        viewModel.navigationEvent.observe(this) { event ->
            navigationManager.handleNavigation(event)
        }

        viewModel.navigationEvent.observe(this) { event ->
            navigationManager.handleNavigationState(event)
            findViewById<View>(R.id.container).visibility =
                if (event is SearchViewModel.NavigationState.Close ||
                    (event is SearchViewModel.NavigationState.Back && navigationManager.isRootFragment())
                ) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        viewModel.profiles.observe(this) { profiles ->
            val validProfiles = profiles.filter { !it.profile_path.isNullOrEmpty() }
            recyclerViewProfiles.visibility =
                if (validProfiles.isEmpty()) View.GONE else View.VISIBLE
            profileAdapter.submitData(validProfiles)
        }

        viewModel.moviesAndShows.observe(this) { pair ->
            movieAdapter.submitData(pair.first, pair.second)
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                buttonSearch.isEnabled = !isLoading
            }
        }
    }

    private fun setupSearch() {
        editTextSearch.doAfterTextChanged { text ->
            if (!text.isNullOrBlank()) {
                viewModel.search(text.toString())
            }
        }

        buttonSearch.setOnClickListener {
            val query = editTextSearch.text.toString()
            if (query.isNotEmpty()) {
                hideKeyboard()
                viewModel.search(query)
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        val fragmentTag = when (fragment) {
            is PosterFragment -> {
                val args = fragment.arguments
                when {
                    args?.containsKey("movie") == true ->
                        "poster_movie_${args.getParcelable<Movie>("movie")?.id}"

                    args?.containsKey("tv") == true ->
                        "poster_tv_${args.getParcelable<TV>("tv")?.id}"

                    else -> "poster_unknown"
                }
            }

            is PersonDetailFragment -> "person_${fragment.arguments?.getInt("person_id")}"
            else -> fragment.javaClass.simpleName
        }

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, fragment, fragmentTag)
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

    override fun onDestroy() {
        recyclerViewMovies.clearOnScrollListeners()
        recyclerViewMovies.adapter = null
        recyclerViewProfiles.adapter = null
        super.onDestroy()
    }
}
