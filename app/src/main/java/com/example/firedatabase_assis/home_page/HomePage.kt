package com.example.firedatabase_assis.home_page

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.Post
import com.example.firedatabase_assis.database.Posts
import com.example.firedatabase_assis.databinding.ActivityHomePageBinding
import com.example.firedatabase_assis.explore.LoadVideos
import com.example.firedatabase_assis.search.SearchActivity
import com.example.firedatabase_assis.settings.SettingsActivity
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


class HomePage : AppCompatActivity() {
    private lateinit var binding: ActivityHomePageBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyPostAdapter

    // Mutable list to hold the posts data
    private var postData: MutableList<Post> = mutableListOf()

    // Flag to indicate whether data is currently being loaded
    private var isLoading = false

    private var offset = 0
    private val limit = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyPostAdapter(this, movies = postData)
        recyclerView.adapter = adapter

        loadData()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    loadData()
                }
            }
        })

        binding.bottomNavBar.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_menu_home -> {
                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                    true
                }

                R.id.bottom_menu_explore -> {
                    val intent = Intent(this, LoadVideos::class.java)
                    startActivity(intent)
                    true
                }

                R.id.bottom_menu_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.bottom_menu_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadData() {
        isLoading = true

        lifecycleScope.launch {
            val newMovies = fetchPostsFromDatabase(limit, offset)
            if (newMovies.isNotEmpty()) {
                postData.addAll(newMovies)
                adapter.notifyDataSetChanged()
                offset += limit
            }
            isLoading = false
        }
    }

    private suspend fun fetchPostsFromDatabase(limit: Int, offset: Int): List<Post> {
        return transaction {
            Posts.selectAll()
                .limit(limit, offset.toLong())
                .map { row ->
                    Post(
                        postId = row[Posts.postId],
                        tmdbId = row[Posts.tmdbId],
                        type = row[Posts.type],
                        title = row[Posts.title],
                        subscription = row[Posts.subscription],
                        releaseDate = row[Posts.releaseDate],
                        overview = row[Posts.overview],
                        posterPath = row[Posts.posterPath],
                        voteAverage = row[Posts.voteAverage],
                        voteCount = row[Posts.voteCount],
                        originalLanguage = row[Posts.originalLanguage],
                        originalTitle = row[Posts.originalTitle],
                        popularity = row[Posts.popularity],
                        genreIds = row[Posts.genreIds],
                        videoKey = row[Posts.videoKey]
                    )
                }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        if (fragmentContainer != null && fragmentContainer.visibility == View.VISIBLE) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment is CommentFragment) {
                val result = fragment.dispatchTouchEvent(event)
                if (result) {
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
