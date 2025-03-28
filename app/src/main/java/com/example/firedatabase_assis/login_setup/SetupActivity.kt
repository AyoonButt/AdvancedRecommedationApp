package com.example.firedatabase_assis.login_setup


import SpinnerUtils
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.GenreEntity
import com.example.firedatabase_assis.postgres.Genres
import com.example.firedatabase_assis.postgres.Providers
import com.example.firedatabase_assis.postgres.SubscriptionProvider
import com.example.firedatabase_assis.postgres.UserDto
import com.example.firedatabase_assis.postgres.UserRequest
import com.example.firedatabase_assis.postgres.Users
import com.example.firedatabase_assis.settings.GenreAdapter
import com.example.firedatabase_assis.settings.SubscriptionAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar


class
SetupActivity : AppCompatActivity() {

    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerRegion: Spinner
    private lateinit var textViewSelectedOldestDate: TextView
    private lateinit var textViewSelectedMostRecentDate: TextView
    private lateinit var linearLayoutSubscriptions: LinearLayout
    private lateinit var recyclerViewGenres: RecyclerView
    private lateinit var recyclerViewSubscriptions: RecyclerView
    private lateinit var linearLayoutProviders: LinearLayout
    private lateinit var linearLayoutGenresSearch: LinearLayout
    private lateinit var buttonSave: Button
    private lateinit var editTextAddSubscription: EditText
    private lateinit var editTextAddGenre: EditText
    private lateinit var editTextAvoidGenres: EditText
    private lateinit var linearLayoutAvoidGenres: LinearLayout
    private lateinit var buttonAddGenre: Button
    private lateinit var textViewSelectedGenres: TextView
    private lateinit var saveSettings: Button
    private lateinit var spinnerMin: Spinner
    private lateinit var spinnerMax: Spinner
    private lateinit var spinnerMinTV: Spinner
    private lateinit var spinnerMaxTV: Spinner

    private lateinit var genresAdapter: GenreAdapter
    private lateinit var subscriptionsAdapter: SubscriptionAdapter
    private val genresList = mutableListOf<GenreItem>()
    private val subscriptionsList = mutableListOf<SubscriptionItem>()

    private var selectedOldestDate: String = ""
    private var selectedMostRecentDate: String = ""

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val providersApi: Providers = retrofit.create(Providers::class.java)
    private val genresApi: Genres = retrofit.create(Genres::class.java)
    private val usersApi: Users = retrofit.create(Users::class.java)


    private val selectedGenres = mutableListOf<String>()


    private val genres = mutableListOf(
        GenreEntity(28, "Action"),
        GenreEntity(35, "Comedy"),
        GenreEntity(18, "Drama"),
        GenreEntity(14, "Fantasy"),
        GenreEntity(27, "Horror"),
        GenreEntity(9648, "Mystery"),
        GenreEntity(53, "Thriller"),
        GenreEntity(10749, "Romance")
    )

    private val minTvRanges = listOf(
        15 to "15 min",
        20 to "20 min",
        25 to "25 min",
        30 to "30 min",
        35 to "35 min",
        40 to "40 min",
        45 to "45 min",
        50 to "50 min"
    )

    private val maxTvRanges = listOf(
        30 to "30 min",
        35 to "35 min",
        40 to "40 min",
        45 to "45 min",
        50 to "50 min",
        55 to "55 min",
        60 to "1 hour",
        70 to "70 min",
        80 to "80 min"
    )

    // Movie duration ranges (minutes)
    private val minMovieRanges = listOf(
        60 to "1 hour",
        75 to "1.25 hours",
        90 to "1.5 hours",
        105 to "1.75 hours",
        120 to "2 hours"
    )

    private val maxMovieRanges = listOf(
        120 to "2 hours",
        135 to "2.25 hours",
        150 to "2.5 hours",
        165 to "2.75 hours",
        180 to "3 hours",
        210 to "3.5 hours",
        240 to "4 hours"
    )


    private var isSaved = false

    private lateinit var providers: List<Provider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)


        val name = intent.getStringExtra("name")
        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val password = intent.getStringExtra("password")

        Log.d("SetupActivity", "Name: $name")
        Log.d("SetupActivity", "Username: $username")
        Log.d("SetupActivity", "Email: $email")
        Log.d("SetupActivity", "Password: $password")


        // Retrieve providers from database

        // Initialize views
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        spinnerRegion = findViewById(R.id.spinnerRegion)
        textViewSelectedOldestDate = findViewById(R.id.textViewSelectedOldestDate)
        textViewSelectedMostRecentDate = findViewById(R.id.textViewSelectedMostRecentDate)
        linearLayoutSubscriptions = findViewById(R.id.linearLayoutSubscriptions)
        recyclerViewGenres = findViewById(R.id.recyclerViewGenres)
        recyclerViewSubscriptions = findViewById(R.id.recyclerViewSubscriptions)
        buttonSave = findViewById(R.id.buttonSave)
        editTextAddSubscription = findViewById(R.id.editTextAddSubscription)
        editTextAddGenre = findViewById(R.id.editTextAddGenre)
        editTextAvoidGenres = findViewById(R.id.editTextAvoidGenres)
        linearLayoutProviders = findViewById(R.id.linearLayoutProviders)
        linearLayoutGenresSearch = findViewById(R.id.linearLayoutGenresSearch)
        linearLayoutAvoidGenres = findViewById(R.id.linearLayoutAvoidGenres)
        buttonAddGenre = findViewById(R.id.buttonAddGenre)
        textViewSelectedGenres = findViewById(R.id.textViewSelectedGenres)
        saveSettings = findViewById(R.id.saveSettings)
        spinnerMin = findViewById(R.id.spinner_min)
        spinnerMax = findViewById(R.id.spinner_max)
        spinnerMinTV = findViewById(R.id.spinner_minTV)
        spinnerMaxTV = findViewById(R.id.spinner_maxTV)

        recyclerViewGenres.layoutManager = LinearLayoutManager(this)
        genresAdapter = GenreAdapter(genresList)
        recyclerViewGenres.adapter = genresAdapter

        val genresTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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
        genresTouchHelper.attachToRecyclerView(recyclerViewGenres)
        genresAdapter.setItemTouchHelper(genresTouchHelper)

        genres.forEach { genre ->
            genresList.add(GenreItem(genre.genreId, genre.genreName))
        }


        recyclerViewSubscriptions.layoutManager = LinearLayoutManager(this)
        subscriptionsAdapter = SubscriptionAdapter(subscriptionsList)
        recyclerViewSubscriptions.adapter = subscriptionsAdapter

        // Set up ItemTouchHelper for drag-and-drop and swipe for subscriptions
        val subscriptionsTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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
                val item = subscriptionsList.removeAt(fromPos)
                subscriptionsList.add(toPos, item)
                subscriptionsAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                subscriptionsList.removeAt(position)
                subscriptionsAdapter.notifyItemRemoved(position)
            }
        })
        subscriptionsTouchHelper.attachToRecyclerView(recyclerViewSubscriptions)
        subscriptionsAdapter.setItemTouchHelper(subscriptionsTouchHelper)


        // Set up save button click listener
        buttonSave.setOnClickListener {
            handleSaveButtonClick()
        }

        // Add subscription button click listener
        findViewById<Button>(R.id.buttonAddSubscription).setOnClickListener {
            handleAddSubscriptionButtonClick()
        }

        // Add genre button click listener
        findViewById<Button>(R.id.buttonAddGenre).setOnClickListener {
            handleAddGenreButtonClick()
        }

        // Populate spinners with data
        setupLanguageSpinner()
        setupRegionSpinner()

        // Setup TV show duration spinners
        val minTvAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            minTvRanges.map { it.second } // Display the formatted time strings
        )
        minTvAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMinTV.adapter = minTvAdapter

        val maxTvAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            maxTvRanges.map { it.second } // Display the formatted time strings
        )
        maxTvAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMaxTV.adapter = maxTvAdapter


        // Setup Movie duration spinners
        val minMovieAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            minMovieRanges.map { it.second } // Display the formatted time strings
        )
        minMovieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMin.adapter = minMovieAdapter

        val maxMovieAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            maxMovieRanges.map { it.second } // Display the formatted time strings
        )
        maxMovieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMax.adapter = maxMovieAdapter


        // Set listeners to ensure min <= max for TV shows
        spinnerMinTV.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedMinValue = minTvRanges[position].first
                val currentMaxPosition = spinnerMaxTV.selectedItemPosition
                val currentMaxValue = maxTvRanges[currentMaxPosition].first

                // If min > max, adjust max
                if (selectedMinValue > currentMaxValue) {
                    // Find the first max value that's >= the selected min value
                    val newMaxPosition = maxTvRanges.indexOfFirst { it.first >= selectedMinValue }
                    if (newMaxPosition != -1) {
                        spinnerMaxTV.setSelection(newMaxPosition)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        // Set listeners to ensure min <= max for Movies
        spinnerMin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedMinValue = minMovieRanges[position].first
                val currentMaxPosition = spinnerMax.selectedItemPosition
                val currentMaxValue = maxMovieRanges[currentMaxPosition].first

                // If min > max, adjust max
                if (selectedMinValue > currentMaxValue) {
                    // Find the first max value that's >= the selected min value
                    val newMaxPosition =
                        maxMovieRanges.indexOfFirst { it.first >= selectedMinValue }
                    if (newMaxPosition != -1) {
                        spinnerMax.setSelection(newMaxPosition)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }


        findViewById<Button>(R.id.buttonSelectOldestDate).setOnClickListener {
            showDatePickerDialog { date ->
                selectedOldestDate = date
                textViewSelectedOldestDate.text = "Selected Oldest Date: $date"
            }
        }

        findViewById<Button>(R.id.buttonSelectMostRecentDate).setOnClickListener {
            showDatePickerDialog { date ->
                selectedMostRecentDate = date
                textViewSelectedMostRecentDate.text = "Selected Most Recent Date: $date"
            }
        }


        editTextAddSubscription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterProviders(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editTextAddSubscription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                linearLayoutProviders.visibility = View.VISIBLE
            } else if (editTextAddSubscription.text.isEmpty()) {
                linearLayoutProviders.visibility = View.GONE
            }
        }

        editTextAddGenre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()

                // Only search if field has focus to avoid conflicts
                if (editTextAddGenre.hasFocus()) {
                    filterGenres(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        editTextAddGenre.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Show genre search container and hide avoid container
                linearLayoutGenresSearch.visibility = View.VISIBLE
                linearLayoutAvoidGenres.visibility = View.GONE

                // If there's text, trigger a search
                if (editTextAddGenre.text.isNotEmpty()) {
                    filterGenres(editTextAddGenre.text.toString())
                }
            } else {
                // Only hide if there's no text
                if (editTextAddGenre.text.isEmpty()) {
                    linearLayoutGenresSearch.visibility = View.GONE
                }
            }
        }

        editTextAvoidGenres.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()

                // Only search if field has focus to avoid conflicts
                if (editTextAvoidGenres.hasFocus()) {
                    filterAvoidGenres(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editTextAvoidGenres.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Show avoid container and hide genre search container
                linearLayoutAvoidGenres.visibility = View.VISIBLE
                linearLayoutGenresSearch.visibility = View.GONE

                // If there's text, trigger a search
                if (editTextAvoidGenres.text.isNotEmpty()) {
                    filterAvoidGenres(editTextAvoidGenres.text.toString())
                }
            } else {
                // Only hide if there's no text
                if (editTextAvoidGenres.text.isEmpty()) {
                    linearLayoutAvoidGenres.visibility = View.GONE
                }
            }
        }

        findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            val query = editTextAvoidGenres.text.toString().trim()
            if (query.isNotEmpty()) {
                // First check if genre exists in preferred genres
                val existsInPreferred = genresList.any { it.name == query }

                if (existsInPreferred) {
                    // If genre exists in preferred list, ask user what to do
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Genre Conflict")
                        .setMessage("'$query' is already in your preferred genres. Do you want to move it to avoided genres instead?")
                        .setPositiveButton("Yes") { _, _ ->
                            // Remove from preferred genres
                            val indexToRemove = genresList.indexOfFirst { it.name == query }
                            if (indexToRemove != -1) {
                                genresList.removeAt(indexToRemove)
                                genresAdapter.notifyItemRemoved(indexToRemove)
                            }

                            // Add to avoid genres
                            if (!selectedGenres.contains(query)) {
                                selectedGenres.add(query)
                                updateSelectedGenresTextView()
                            }

                            editTextAvoidGenres.setText("")
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    // Normal flow - just add to avoid genres
                    if (!selectedGenres.contains(query)) {
                        selectedGenres.add(query)
                        updateSelectedGenresTextView()
                        editTextAvoidGenres.setText("")
                    }
                }
            }
        }

        findViewById<Button>(R.id.saveSettings).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val newUser = prepareUserData()
                saveUser(newUser)
            }
        }

    }

    private fun filterProviders(query: String) {
        if (query.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Asynchronously call the API to filter genres
                    val response = providersApi.filterProviders(query)
                    if (response.isSuccessful) {
                        val filteredProviders = response.body() ?: emptyList()
                        withContext(Dispatchers.Main) {
                            updateProviders(filteredProviders)
                        }
                    } else {
                        Log.e(
                            "ProvidersManager",
                            "Failed to fetch filtered Providers: ${response.message()}"
                        )
                        withContext(Dispatchers.Main) {
                            updateProviders(emptyList())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProvidersManager", "Error fetching filtered providers", e)
                    withContext(Dispatchers.Main) {
                        updateProviders(emptyList())
                    }
                }
            }
        } else {
            updateProviders(emptyList())
        }
    }


    private fun updateProviders(newProviders: List<SubscriptionProvider>) {
        linearLayoutProviders.removeAllViews()
        newProviders.forEach { provider ->
            val providerView = LayoutInflater.from(this)
                .inflate(R.layout.provider_item, linearLayoutProviders, false)
            val textViewProviderName = providerView.findViewById<TextView>(R.id.provider_name)

            textViewProviderName.text = provider.providerName

            providerView.setOnClickListener {
                // Set the provider name in the edit text
                editTextAddSubscription.setText(provider.providerName)
                linearLayoutProviders.removeAllViews()
            }
            linearLayoutProviders.addView(providerView)
        }
    }


    private fun filterGenres(query: String) {
        if (query.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Asynchronously call the API to filter genres
                    val response = genresApi.filterGenres(query)
                    if (response.isSuccessful) {
                        val filteredGenres = response.body() ?: emptyList()
                        withContext(Dispatchers.Main) {
                            // Update the search results container
                            updateGenresSearchResults(filteredGenres)
                        }
                    } else {
                        Log.e(
                            "GenresManager",
                            "Failed to fetch filtered genres: ${response.message()}"
                        )
                        withContext(Dispatchers.Main) {
                            // Clear container on error
                            linearLayoutGenresSearch.removeAllViews()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GenresManager", "Error fetching filtered genres", e)
                    withContext(Dispatchers.Main) {
                        // Clear container on error
                        linearLayoutGenresSearch.removeAllViews()
                    }
                }
            }
        } else {
            // Clear container when query is empty
            linearLayoutGenresSearch.removeAllViews()
        }
    }

    private fun updateGenresSearchResults(newGenres: List<GenreEntity>) {
        // Always use the genres search container for regular genre search results
        linearLayoutGenresSearch.removeAllViews()
        linearLayoutGenresSearch.visibility = if (newGenres.isEmpty()) View.GONE else View.VISIBLE

        newGenres.forEach { genre ->
            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutGenresSearch, false)
            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            textViewGenreName.text = genre.genreName

            genreView.setOnClickListener {
                // Only set the text, don't add to list
                editTextAddGenre.setText(genre.genreName)

                // Clear the search results
                linearLayoutGenresSearch.visibility = View.GONE
            }
            linearLayoutGenresSearch.addView(genreView)
        }
    }

    private fun filterAvoidGenres(query: String) {
        if (query.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Asynchronously call the API to filter avoid genres
                    val response = genresApi.filterAvoidGenres(query)
                    if (response.isSuccessful) {
                        val filteredAvoidGenres = response.body() ?: emptyList()
                        withContext(Dispatchers.Main) {
                            // Make sure we're updating the correct container
                            updateAvoidGenresResults(filteredAvoidGenres)
                        }
                    } else {
                        Log.e(
                            "DataManager",
                            "Failed to fetch filtered avoid genres: ${response.message()}"
                        )
                        withContext(Dispatchers.Main) {
                            // Clear the avoid genres container on error
                            linearLayoutAvoidGenres.removeAllViews()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DataManager", "Error fetching filtered avoid genres", e)
                    withContext(Dispatchers.Main) {
                        // Clear the avoid genres container on error
                        linearLayoutAvoidGenres.removeAllViews()
                    }
                }
            }
        } else {
            // Clear the avoid genres container when query is empty
            linearLayoutAvoidGenres.removeAllViews()
        }
    }

    private fun updateAvoidGenresResults(newGenres: List<GenreEntity>) {
        // Always use the avoid genres container for avoid genre search results
        linearLayoutAvoidGenres.removeAllViews()
        linearLayoutAvoidGenres.visibility = if (newGenres.isEmpty()) View.GONE else View.VISIBLE

        newGenres.forEach { genre ->
            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutAvoidGenres, false)
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

                            // Clear input field and search results
                            editTextAvoidGenres.setText("")
                            linearLayoutAvoidGenres.visibility = View.GONE
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    // Normal flow - just set the text and hide results
                    editTextAvoidGenres.setText(genre.genreName)
                    linearLayoutAvoidGenres.visibility = View.GONE
                }
            }
            linearLayoutAvoidGenres.addView(genreView)
        }
    }


    private fun updateSelectedGenresTextView() {
        val genresText = selectedGenres.joinToString(", ")
        textViewSelectedGenres.text = "Selected Genres: $genresText"
    }

    private fun handleSaveButtonClick() {
        val checkedSubscriptions = mutableListOf<String>()

        // Collect checked subscriptions
        for (i in 0 until linearLayoutSubscriptions.childCount) {
            val view = linearLayoutSubscriptions.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                checkedSubscriptions.add(view.text.toString())
            }
        }

        // Clear existing adapter data
        subscriptionsList.clear()

        // Add checked subscriptions to the RecyclerView's data list
        checkedSubscriptions.forEach { subscription ->
            // Using placeholder ID of 0; you'll need to get the real ID from API
            subscriptionsList.add(SubscriptionItem(0, subscription))
        }

        // Notify adapter of changes
        subscriptionsAdapter.notifyDataSetChanged()

        // Hide checkbox layout and show RecyclerView
        linearLayoutSubscriptions.visibility = View.GONE
        recyclerViewSubscriptions.visibility = View.VISIBLE
        buttonSave.visibility = View.GONE

        // Set the flag to indicate that the save button has been pressed
        isSaved = true
    }


    private fun handleAddSubscriptionButtonClick() {
        val subscriptionName = editTextAddSubscription.text.toString().trim()
        if (subscriptionName.isNotEmpty()) {
            if (isSaved) {
                // Add to RecyclerView list
                subscriptionsList.add(SubscriptionItem(0, subscriptionName))
                subscriptionsAdapter.notifyItemInserted(subscriptionsList.size - 1)
            } else {
                // Add as checkbox
                val checkBox = CheckBox(this)
                checkBox.text = subscriptionName
                linearLayoutSubscriptions.addView(checkBox)
            }
            editTextAddSubscription.text.clear()
        }
    }

    private fun handleAddGenreButtonClick() {
        val genreName = editTextAddGenre.text.toString().trim()
        if (genreName.isNotEmpty()) {
            // Check if genre already exists in the list to avoid duplicates
            if (!genresList.any { it.name.equals(genreName, ignoreCase = true) }) {
                // Add to preferred genres list
                genresList.add(GenreItem(0, genreName))
                genresAdapter.notifyItemInserted(genresList.size - 1)

                // Check if this genre exists in avoid genres and remove it
                if (selectedGenres.contains(genreName)) {
                    selectedGenres.remove(genreName)
                    updateSelectedGenresTextView()
                    Toast.makeText(
                        this@SetupActivity,
                        "Removed '$genreName' from avoided genres",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Notify user that genre already exists
                Toast.makeText(
                    this@SetupActivity,
                    "'$genreName' is already in your preferred genres",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Clear the input field
            editTextAddGenre.text.clear()

            // Hide the search results
            linearLayoutGenresSearch.removeAllViews()
            linearLayoutGenresSearch.visibility = View.GONE
        }
    }

    private fun setupLanguageSpinner() {
        SpinnerUtils.setupLanguageSpinner(this, spinnerLanguage)
    }

    private fun setupRegionSpinner() {
        SpinnerUtils.setupRegionSpinner(this, spinnerRegion)
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = "$selectedYear/${selectedMonth + 1}/$selectedDay"
                onDateSelected(date)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private suspend fun getProviderIds(names: List<String>): List<Int> {
        // Call the API to fetch provider IDs based on provider names
        val response = providersApi.getProviderIdsByNames(names)

        // Check if the response is successful
        return if (response.isSuccessful) {
            // Return the list of provider IDs, or an empty list if the body is null
            response.body() ?: emptyList()
        } else {
            // Handle the error case (e.g., log it or throw an exception)
            emptyList()  // Return an empty list or throw an exception based on your needs
        }
    }

    private suspend fun getGenreIds(names: List<String>): List<Int> {
        // Call the API to fetch provider IDs based on provider names
        val response = genresApi.getGenreIdsByNames(names)

        // Check if the response is successful
        return if (response.isSuccessful) {
            // Return the list of provider IDs, or an empty list if the body is null
            response.body() ?: emptyList()
        } else {
            // Handle the error case (e.g., log it or throw an exception)
            emptyList()  // Return an empty list or throw an exception based on your needs
        }
    }


    private fun getOrderOfItemNames(): List<String> {
        return genresList.map { it.name }
    }

    private fun getOrderOfSubscriptionItemNames(): List<String> {
        // If we're still in checkbox mode, return empty list or handle appropriately
        if (!isSaved) {
            // You might want to collect checked item names here
            return emptyList()
        }

        // If we're in RecyclerView mode, get names from the adapter's data
        return subscriptionsList.map { it.name }
    }

    private fun getCurrentTimestamp(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    private suspend fun prepareUserData(): UserRequest {
        // Retrieve user registration data
        val name_string = intent.getStringExtra("name") ?: ""
        val username_string = intent.getStringExtra("username") ?: ""
        val email_string = intent.getStringExtra("email") ?: ""
        val password = intent.getStringExtra("password") ?: ""

        // Retrieve settings data
        val selectedLanguage = spinnerLanguage.selectedItem.toString()
        val selectedRegion = spinnerRegion.selectedItem.toString()
        val languageId = getIso6391(selectedLanguage)
        val countryId = getIsoCode(selectedRegion)

        val selectedMin = minMovieRanges[spinnerMin.selectedItemPosition].first
        val selectedMax = maxMovieRanges[spinnerMax.selectedItemPosition].first
        val selectedMinTV = minTvRanges[spinnerMinTV.selectedItemPosition].first
        val selectedMaxTV = maxTvRanges[spinnerMaxTV.selectedItemPosition].first

        val genresToAvoid = selectedGenres.joinToString(", ")
        val subscriptionNames = getOrderOfSubscriptionItemNames()
        val genreNames = getOrderOfItemNames()
        val subscriptionIds = getProviderIds(subscriptionNames)
        val genreIds = getGenreIds(genreNames)

        // Prepare the user data
        val user = UserDto(
            userId = null,
            name = name_string,
            username = username_string,
            email = email_string,
            password = password,
            language = languageId,
            region = countryId ?: "Default",
            minMovie = selectedMin,
            maxMovie = selectedMax,
            minTv = selectedMinTV,
            maxTv = selectedMaxTV,
            oldestDate = selectedOldestDate,
            recentDate = selectedMostRecentDate,
            recentLogin = "NULL",
            createdAt = getCurrentTimestamp(),
        )

        // Return the UserRequest
        return UserRequest(
            userDto = user,
            subscriptions = subscriptionIds,
            genres = genreIds,
            avoidGenres = genresToAvoid.split(", ")
                .mapNotNull { it.toIntOrNull() } // Convert to list of integers and filter out invalid values
                .filter { it != 0 }
        )
    }


    private suspend fun saveUser(userRequest: UserRequest) {
        // Directly call the API to save the user
        val response = usersApi.addUser(userRequest)

        // Check if the response is successful
        if (response.isSuccessful) {
            // Handle successful user creation (e.g., navigate to login)
            // Navigate to Login
            val intent = Intent(this, LoginForm::class.java)
            startActivity(intent)
            finish()
        } else {
            Log.e(
                "UserCreation",
                "API call failed: Code ${response.code()}, Message: ${response.message()}, Body: ${
                    response.errorBody()?.string()
                }"
            )

        }
    }


}
