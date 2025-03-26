package com.example.firedatabase_assis.settings

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityGenresBinding
import com.example.firedatabase_assis.login_setup.GenreItem
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.GenreEntity
import com.example.firedatabase_assis.postgres.Genres
import com.example.firedatabase_assis.postgres.UserGenreDto
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GenresActivity : BaseActivity() {
    private lateinit var binding: ActivityGenresBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var genresAdapter: GenreAdapter
    private lateinit var searchAdapter: GenreSearchAdapter
    private lateinit var genresList: MutableList<GenreItem>
    private lateinit var editTextAddGenre: EditText
    private lateinit var editTextAvoidGenres: EditText
    private lateinit var textViewSelectedGenres: TextView
    private val selectedGenres = mutableListOf<String>()

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

        ActivityNavigationHelper.setLastOpenedSettingsActivity(this::class.java)

        userViewModel = UserViewModel.getInstance(application)
        genresList = mutableListOf()

        editTextAddGenre = binding.editTextAddGenre
        editTextAvoidGenres = binding.editTextAvoidGenres
        textViewSelectedGenres = binding.textViewSelectedGenres

        setupToolbar("Genres")
        setupRecyclerViews()
        loadUserGenres()
        loadAvoidGenres()
        setupSearchGenre()
        setupAvoidGenreSearch()
        setupSaveButton()
        setupAddAvoidGenreButton()
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setTitle(title)
        }
        binding.toolbar.setNavigationOnClickListener {
            // Navigate back to SettingsActivity
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityNavigationHelper.removeLastOpenedSettingsActivity()
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerViews() {
        // Setup genres RecyclerView
        val recyclerViewGenres = binding.recyclerViewGenres
        recyclerViewGenres.layoutManager = LinearLayoutManager(this)
        genresAdapter = GenreAdapter(genresList)
        recyclerViewGenres.adapter = genresAdapter

        // Setup ItemTouchHelper for drag-and-drop and swipe
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                // Update the data
                val item = genresList.removeAt(fromPos)
                genresList.add(toPos, item)
                genresAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                genresList.removeAt(position)
                genresAdapter.notifyItemRemoved(position)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerViewGenres)
        genresAdapter.setItemTouchHelper(itemTouchHelper)

        // Setup search RecyclerView
        val recyclerViewSearch = binding.recyclerViewGenreSearch
        recyclerViewSearch.layoutManager = LinearLayoutManager(this)
        searchAdapter = GenreSearchAdapter { genre ->
            // Add the selected genre to user's list
            if (!genresList.any { it.name == genre.genreName }) {
                genresList.add(GenreItem(genre.genreId, genre.genreName))
                genresAdapter.notifyItemInserted(genresList.size - 1)

                // Check if this genre exists in avoid genres and remove it
                if (selectedGenres.contains(genre.genreName)) {
                    selectedGenres.remove(genre.genreName)
                    updateSelectedGenresTextView()
                    Toast.makeText(
                        this@GenresActivity,
                        "Removed '${genre.genreName}' from avoided genres",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                editTextAddGenre.setText("")
                binding.recyclerViewGenreSearch.visibility = View.GONE
            }
        }
        recyclerViewSearch.adapter = searchAdapter
    }

    private fun loadUserGenres() {
        lifecycleScope.launch {
            try {
                userViewModel.currentUser.value?.let { currentUser ->
                    val response = genresApi.getUserGenres(currentUser.userId)

                    if (response.isSuccessful) {
                        val genres = response.body() ?: emptyList()
                        genresList.clear()

                        genres.sortedBy { it.priority }.forEach { genre ->
                            genresList.add(GenreItem(genre.genreId, genre.genreName))
                        }

                        genresAdapter.notifyDataSetChanged()
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

    private fun loadAvoidGenres() {
        lifecycleScope.launch {
            try {
                userViewModel.currentUser.value?.let { currentUser ->
                    val response = genresApi.getUserAvoidGenres(currentUser.userId)

                    if (response.isSuccessful) {
                        val genres = response.body() ?: emptyList()
                        selectedGenres.clear()

                        genres.forEach { genre ->
                            selectedGenres.add(genre.genreName)
                        }

                        updateSelectedGenresTextView()
                    } else {
                        throw Exception("Failed to fetch avoid genres")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@GenresActivity,
                    "Error loading avoid genres: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupSearchGenre() {
        editTextAddGenre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    binding.recyclerViewGenreSearch.visibility = View.VISIBLE
                    filterGenres(query)
                } else {
                    binding.recyclerViewGenreSearch.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAvoidGenreSearch() {
        editTextAvoidGenres.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    binding.linearLayoutAvoidGenres.visibility = View.VISIBLE
                    filterAvoidGenres(query)
                } else {
                    binding.linearLayoutAvoidGenres.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterGenres(query: String) {
        lifecycleScope.launch {
            try {
                val response = genresApi.filterGenres(query)
                if (response.isSuccessful) {
                    val filteredGenres = response.body() ?: emptyList()
                    searchAdapter.updateGenres(filteredGenres)
                } else {
                    searchAdapter.updateGenres(emptyList())
                }
            } catch (e: Exception) {
                Log.e("GenresActivity", "Error filtering genres", e)
                searchAdapter.updateGenres(emptyList())
            }
        }
    }

    private fun filterAvoidGenres(query: String) {
        lifecycleScope.launch {
            try {
                val response = genresApi.filterGenres(query)
                if (response.isSuccessful) {
                    val filteredGenres = response.body() ?: emptyList()
                    updateAvoidGenres(filteredGenres)
                } else {
                    updateAvoidGenres(emptyList())
                }
            } catch (e: Exception) {
                Log.e("GenresActivity", "Error filtering avoid genres", e)
                updateAvoidGenres(emptyList())
            }
        }
    }

    private fun updateAvoidGenres(newGenres: List<GenreEntity>) {
        binding.linearLayoutAvoidGenres.removeAllViews()
        newGenres.forEach { genre ->
            val genreView = layoutInflater.inflate(
                R.layout.genre_search_item,
                binding.linearLayoutAvoidGenres,
                false
            )
            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            textViewGenreName.text = genre.genreName
            genreView.setOnClickListener {
                // Check if this genre exists in preferred genres
                val existsInPreferred = genresList.any { it.name == genre.genreName }

                if (existsInPreferred) {
                    // If genre exists in preferred list, ask user what to do
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Genre Conflict")
                        .setMessage("'${genre.genreName}' is already in your preferred genres. Do you want to move it to avoided genres instead?")
                        .setPositiveButton("Yes") { _, _ ->
                            // Remove from preferred genres
                            val indexToRemove =
                                genresList.indexOfFirst { it.name == genre.genreName }
                            if (indexToRemove != -1) {
                                genresList.removeAt(indexToRemove)
                                genresAdapter.notifyItemRemoved(indexToRemove)
                            }

                            // Add to avoid genres
                            if (!selectedGenres.contains(genre.genreName)) {
                                selectedGenres.add(genre.genreName)
                                updateSelectedGenresTextView()
                            }

                            editTextAvoidGenres.setText("")
                            binding.linearLayoutAvoidGenres.visibility = View.GONE
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    // Normal flow - just set the text and hide results
                    editTextAvoidGenres.setText(genre.genreName)
                    binding.linearLayoutAvoidGenres.visibility = View.GONE
                }
            }
            binding.linearLayoutAvoidGenres.addView(genreView)
        }
    }

    private fun updateSelectedGenresTextView() {
        val genresText = selectedGenres.joinToString(", ")
        textViewSelectedGenres.text = "Genres to Avoid: $genresText"
    }

    private fun setupAddAvoidGenreButton() {
        binding.buttonAdd.setOnClickListener {
            val genreName = editTextAvoidGenres.text.toString().trim()
            if (genreName.isNotEmpty()) {
                // First check if genre exists in preferred genres
                val existsInPreferred = genresList.any { it.name == genreName }

                if (existsInPreferred) {
                    // If genre exists in preferred list, ask user what to do
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Genre Conflict")
                        .setMessage("'$genreName' is already in your preferred genres. Do you want to move it to avoided genres instead?")
                        .setPositiveButton("Yes") { _, _ ->
                            // Remove from preferred genres
                            val indexToRemove = genresList.indexOfFirst { it.name == genreName }
                            if (indexToRemove != -1) {
                                genresList.removeAt(indexToRemove)
                                genresAdapter.notifyItemRemoved(indexToRemove)
                            }

                            // Add to avoid genres
                            if (!selectedGenres.contains(genreName)) {
                                selectedGenres.add(genreName)
                                updateSelectedGenresTextView()
                            }

                            editTextAvoidGenres.setText("")
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    // Normal flow - just add to avoid genres
                    if (!selectedGenres.contains(genreName)) {
                        selectedGenres.add(genreName)
                        updateSelectedGenresTextView()
                        editTextAvoidGenres.setText("")
                    }
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    userViewModel.currentUser.value?.let { currentUser ->
                        // Update genres
                        val userGenres = genresList.mapIndexed { index, genreItem ->
                            UserGenreDto(
                                userId = currentUser.userId,
                                genreId = genreItem.id,
                                genreName = genreItem.name,
                                priority = index + 1
                            )
                        }

                        val updateGenresResponse = genresApi.updateUserGenres(
                            currentUser.userId,
                            userGenres
                        )

                        // Update avoid genres
                        val avoidGenreIds = getGenreIds(selectedGenres)
                        val updateAvoidGenresResponse = genresApi.updateUserAvoidGenres(
                            currentUser.userId,
                            avoidGenreIds
                        )

                        if (updateGenresResponse.isSuccessful && updateAvoidGenresResponse.isSuccessful) {
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

    private suspend fun getGenreIds(names: List<String>): List<Int> {
        // Call the API to fetch genre IDs based on genre names
        val response = genresApi.getGenreIdsByNames(names)

        // Check if the response is successful
        return if (response.isSuccessful) {
            // Return the list of genre IDs, or an empty list if the body is null
            response.body() ?: emptyList()
        } else {
            // Handle the error case
            emptyList()
        }
    }
}