package com.example.firedatabase_assis.settings

import android.content.ClipData
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.DragEvent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityGenresBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.GenreEntity
import com.example.firedatabase_assis.postgres.Genres
import com.example.firedatabase_assis.postgres.UserGenreDto
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GenresActivity : BaseActivity() {
    private lateinit var binding: ActivityGenresBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var linearLayoutGenres: LinearLayout
    private lateinit var linearLayoutGenreSearch: LinearLayout
    private lateinit var editTextAddGenre: EditText

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val genresApi: Genres = retrofit.create(Genres::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenresBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)

        userViewModel = UserViewModel.getInstance(application)

        linearLayoutGenres = binding.linearLayoutGenres
        linearLayoutGenreSearch = binding.linearLayoutGenreSearch
        editTextAddGenre = binding.editTextAddGenre

        setupToolbar("Genres")
        setupGenresList()
        setupSearchGenre()
        setupSaveButton()
    }

    private fun setupGenresList() {
        lifecycleScope.launch {
            try {
                userViewModel.currentUser.value?.let { currentUser ->
                    val response = genresApi.getUserGenres(currentUser.userId)

                    if (response.isSuccessful) {
                        val genres = response.body() ?: emptyList()
                        genres.sortedBy { it.priority }.forEach { genre ->
                            val genreView = createGenreView(genre.genreName)
                            linearLayoutGenres.addView(genreView)
                        }
                    } else {
                        throw Exception("Failed to fetch genres")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@GenresActivity,
                    "Error loading genres: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)  // Shows back button
            setDisplayShowTitleEnabled(true) // Shows title
            setTitle(title)  // Sets the title
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun createGenreView(name: String): View {
        val inflater = LayoutInflater.from(this)
        val genreView = inflater.inflate(R.layout.genre_item, linearLayoutGenres, false)
        val textView = genreView.findViewById<TextView>(R.id.textViewGenreName)
        textView.text = name

        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val swipeThreshold = 100
                    val deltaX = e2.x - e1.x

                    if (deltaX > swipeThreshold) { // Right swipe
                        genreView.animate()
                            .translationX(genreView.width.toFloat())
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                linearLayoutGenres.removeView(genreView)
                            }
                        return true
                    }
                    return false
                }
            })

        genreView.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                val shadowBuilder = View.DragShadowBuilder(v)
                val dragData = ClipData.newPlainText("", "")

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    v.startDragAndDrop(dragData, shadowBuilder, v, 0)
                } else {
                    @Suppress("DEPRECATION")
                    v.startDrag(dragData, shadowBuilder, v, 0)
                }

                v.visibility = View.INVISIBLE
                true
            } else {
                false
            }
        }

        return genreView
    }

    private fun setupSearchGenre() {
        editTextAddGenre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterGenres(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editTextAddGenre.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                linearLayoutGenreSearch.visibility = View.VISIBLE
            } else if (editTextAddGenre.text.isEmpty()) {
                linearLayoutGenreSearch.visibility = View.GONE
            }
        }

        linearLayoutGenres.setOnDragListener { v, event ->
            handleDragEvent(v, event, linearLayoutGenres)
        }
    }

    private fun filterGenres(query: String) {
        if (query.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val response = genresApi.filterGenres(query)
                    if (response.isSuccessful) {
                        val filteredGenres = response.body() ?: emptyList()
                        Log.d(
                            "GenresActivity",
                            "API Response: $filteredGenres"
                        )  // Log the entire response
                        filteredGenres.forEach { genre ->
                            Log.d(
                                "GenresActivity",
                                "Genre: ${genre.genreName}"
                            )  // Log each genre name
                        }
                        updateGenres(filteredGenres)
                    }
                } catch (e: Exception) {
                    Log.e("GenresActivity", "Error filtering genres", e)
                    updateGenres(emptyList())
                }
            }
        } else {
            updateGenres(emptyList())
        }
    }

    private fun updateGenres(genres: List<GenreEntity>) {
        Log.d("GenresActivity", "Updating genres with ${genres.size} items")
        linearLayoutGenreSearch.removeAllViews()

        genres.forEach { genre ->
            Log.d("GenresActivity", "Processing genre: ${genre.genreName}")

            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutGenreSearch, false)

            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            if (textViewGenreName == null) {
                Log.e("GenresActivity", "Failed to find TextView with id: genres_name")
                return@forEach
            }

            textViewGenreName.text = genre.genreName
            Log.d("GenresActivity", "Set text to: ${genre.genreName}")

            // Make sure the TextView is visible
            textViewGenreName.visibility = View.VISIBLE

            genreView.setOnClickListener {
                val newGenreView = createGenreView(genre.genreName)
                linearLayoutGenres.addView(newGenreView)
                editTextAddGenre.setText("")
                linearLayoutGenreSearch.removeAllViews()
            }

            linearLayoutGenreSearch.addView(genreView)
            Log.d("GenresActivity", "Added view to layout")
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    userViewModel.currentUser.value?.let { currentUser ->
                        val orderedNames = getOrderOfGenreNames(linearLayoutGenres)
                        val genreIdsResponse = genresApi.getGenreIdsByNames(orderedNames)

                        if (!genreIdsResponse.isSuccessful) {
                            throw Exception("Failed to get genre IDs")
                        }

                        val genreIds = genreIdsResponse.body() ?: emptyList()

                        val genres = genreIds.mapIndexed { index, genreId ->
                            UserGenreDto(
                                userId = currentUser.userId,
                                genreId = genreId,
                                genreName = orderedNames[index],
                                priority = index + 1
                            )
                        }

                        val updateResponse = genresApi.updateUserGenres(
                            currentUser.userId,
                            genres
                        )

                        if (updateResponse.isSuccessful) {
                            Toast.makeText(
                                this@GenresActivity,
                                "Genres updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            throw Exception("Failed to update genres")
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@GenresActivity,
                        "Error updating genres: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleDragEvent(v: View, event: DragEvent, targetLayout: LinearLayout): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                v.invalidate()
                true
            }

            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> {
                v.invalidate()
                true
            }

            DragEvent.ACTION_DROP -> {
                val view = event.localState as View
                val owner = view.parent as LinearLayout
                owner.removeView(view)
                targetLayout.addView(view)
                view.visibility = View.VISIBLE
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                val view = event.localState as View
                view.visibility = View.VISIBLE
                v.invalidate()
                true
            }

            else -> false
        }
    }

    private fun getOrderOfGenreNames(layout: LinearLayout): List<String> {
        val sequence = mutableListOf<String>()
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            val textView = view.findViewById<TextView>(R.id.textViewGenreName)
            textView?.let { sequence.add(it.text.toString()) }
        }
        return sequence
    }
}