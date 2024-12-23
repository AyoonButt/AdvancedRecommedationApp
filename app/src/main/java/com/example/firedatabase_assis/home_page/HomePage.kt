package com.example.firedatabase_assis.home_page

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityHomePageBinding
import com.example.firedatabase_assis.explore.LoadVideos
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostEntity
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.search.SearchActivity
import com.example.firedatabase_assis.settings.SettingsActivity
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomePage : AppCompatActivity() {
    private lateinit var binding: ActivityHomePageBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyPostAdapter
    private lateinit var postsService: Posts

    private var postData: MutableList<PostEntity> = mutableListOf()
    private var isLoading = false
    private var offset = 0
    private val limit = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyPostAdapter(this, movies = postData, UserViewModel())
        recyclerView.adapter = adapter

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        postsService = retrofit.create(Posts::class.java)


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
            try {
                // Make the API call
                val response = postsService.getPaginatedPosts(limit, offset)

                // Check if the response is successful
                if (response.isSuccessful) {
                    val newMovies = response.body() ?: emptyList()

                    if (newMovies.isNotEmpty()) {
                        // Add the new data to the list and update the adapter
                        postData.addAll(newMovies)
                        adapter.notifyDataSetChanged()
                        offset += limit
                    }
                } else {
                    // Log or handle the error response
                    println("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
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

private fun <E> MutableList<E>.addAll(elements: List<PostEntity>) {

}
