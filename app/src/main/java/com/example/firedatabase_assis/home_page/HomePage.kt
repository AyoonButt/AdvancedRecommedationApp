package com.example.firedatabase_assis.home_page

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.MovieDatabase
import kotlinx.coroutines.launch

class HomePage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyPostAdapter

    // Dummy data source
    private var postData: MutableList<Post> = mutableListOf()

    // Flag to indicate whether data is currently being loaded
    private var isLoading = false

    private lateinit var swipeGestureListener: SwipeGestureListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        val database = MovieDatabase.getDatabase(applicationContext)
        val commentDao = database.commentDao()

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyPostAdapter(this, movies = postData, commentDao = commentDao)
        recyclerView.adapter = adapter

        val postId = 0
        val fragmentContainer = findViewById<View>(R.id.fragment_container)

        val swipeGestureListener = SwipeGestureListener(this) {
            fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack()
        }

        fragmentContainer.setOnTouchListener { view, event ->
            swipeGestureListener.onTouch(view, event)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CommentFragment(postId, commentDao))
            .addToBackStack(null)
            .commit()

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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        swipeGestureListener.onTouch(fragmentContainer, event)
        return super.onTouchEvent(event)
    }


    private var offset = 0
    private val limit = 10
    private fun loadData() {
        val database = MovieDatabase.getDatabase(applicationContext)
        val primeDao = database.primeMovieDao()

        lifecycleScope.launch {
            val newMovies = primeDao.getMoviesWithPagination(limit, offset)
            if (newMovies.isNotEmpty()) {
                postData.addAll(newMovies)
                adapter.notifyDataSetChanged()
                offset += limit
            }
        }
    }
}
