package com.example.firedatabase_assis.login_setup


import SpinnerUtils
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.GenreEntity
import com.example.firedatabase_assis.postgres.Genres
import com.example.firedatabase_assis.postgres.Providers
import com.example.firedatabase_assis.postgres.SubscriptionProvider
import com.example.firedatabase_assis.postgres.UserDto
import com.example.firedatabase_assis.postgres.UserRequest
import com.example.firedatabase_assis.postgres.Users
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
    private lateinit var linearLayoutGenres: LinearLayout
    private lateinit var linearLayoutSubscriptions: LinearLayout
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

    private var minValues: List<String> =
        mutableListOf("10", "20", "30", "40", "60", "75", "90", "100", "120")
    private var maxValues: List<String> =
        mutableListOf("60", "75", "90", "100", "120", "150", "180", "210", "240")


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
        linearLayoutGenres = findViewById(R.id.linearLayoutGenres)
        linearLayoutSubscriptions = findViewById(R.id.linearLayoutSubscriptions)
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


        // Add initial custom View elements to LinearLayout
        genres.forEach { genre ->
            val genreView = createGenreView(genre.genreName)
            linearLayoutGenres.addView(genreView)
        }

        // Set up drag listeners for LinearLayouts
        linearLayoutGenres.setOnDragListener { v, event ->
            handleDragEvent(v, event, linearLayoutGenres)
        }
        linearLayoutSubscriptions.setOnDragListener { v, event ->
            handleDragEvent(v, event, linearLayoutSubscriptions)
        }

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

        val minAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, minValues)
        minAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMin.setAdapter(minAdapter)

        val maxAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, maxValues)
        maxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMax.setAdapter(maxAdapter)

        minAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMinTV.setAdapter(minAdapter)

        maxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMaxTV.setAdapter(maxAdapter)




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
                filterGenres(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editTextAddGenre.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                linearLayoutGenresSearch.visibility = View.VISIBLE // Show genre suggestions layout
            } else if (editTextAddGenre.text.isEmpty()) {
                linearLayoutGenresSearch.visibility =
                    View.GONE // Hide genre suggestions layout if empty
            }
        }

        editTextAvoidGenres.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterAvoidGenres(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editTextAvoidGenres.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                linearLayoutGenresSearch.visibility = View.VISIBLE // Show genre suggestions layout
            } else if (editTextAvoidGenres.text.isEmpty()) {
                linearLayoutGenresSearch.visibility =
                    View.GONE // Hide genre suggestions layout if empty
            }
        }

        findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            val query = editTextAvoidGenres.text.toString()
            if (query.isNotEmpty()) {
                selectedGenres.add(query)
                updateSelectedGenresTextView()
                editTextAvoidGenres.text.clear()
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
        newProviders.forEach { providerName ->
            val providerView = LayoutInflater.from(this)
                .inflate(R.layout.provider_item, linearLayoutProviders, false)
            val textViewProviderName = providerView.findViewById<TextView>(R.id.provider_name)
            textViewProviderName.text = providerName.toString()
            providerView.setOnClickListener {
                editTextAddSubscription.setText(providerName.toString())
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
                            updateGenres(filteredGenres)
                        }
                    } else {
                        Log.e(
                            "GenresManager",
                            "Failed to fetch filtered genres: ${response.message()}"
                        )
                        withContext(Dispatchers.Main) {
                            updateGenres(emptyList())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GenresManager", "Error fetching filtered genres", e)
                    withContext(Dispatchers.Main) {
                        updateGenres(emptyList())
                    }
                }
            }
        } else {
            updateGenres(emptyList())
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
                            updateAvoidGenres(filteredAvoidGenres)
                        }
                    } else {
                        Log.e(
                            "DataManager",
                            "Failed to fetch filtered avoid genres: ${response.message()}"
                        )
                        withContext(Dispatchers.Main) {
                            updateAvoidGenres(emptyList())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DataManager", "Error fetching filtered avoid genres", e)
                    withContext(Dispatchers.Main) {
                        updateAvoidGenres(emptyList())
                    }
                }
            }
        } else {
            updateAvoidGenres(emptyList())
        }
    }


    private fun updateGenres(newGenres: List<GenreEntity>) {
        linearLayoutGenresSearch.removeAllViews()
        newGenres.forEach { genre ->
            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutGenresSearch, false)
            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            textViewGenreName.text = genre.genreName
            genreView.setOnClickListener {
                editTextAddGenre.setText(genre.genreName)
                linearLayoutGenresSearch.removeAllViews()
            }
            linearLayoutGenresSearch.addView(genreView)
        }
    }

    private fun updateAvoidGenres(newGenres: List<GenreEntity>) {
        linearLayoutAvoidGenres.removeAllViews()
        newGenres.forEach { genre ->
            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutAvoidGenres, false)
            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            textViewGenreName.text = genre.genreName
            genreView.setOnClickListener {
                editTextAvoidGenres.setText(genre.genreName)
                linearLayoutAvoidGenres.removeAllViews()
            }
            linearLayoutAvoidGenres.addView(genreView)
        }
    }

    private fun updateSelectedGenresTextView() {
        val genresText = selectedGenres.joinToString(", ")
        textViewSelectedGenres.text = "Selected Genres: $genresText"
    }


    private fun createGenreView(name: String): View {
        val inflater = LayoutInflater.from(this)
        val genreView = inflater.inflate(R.layout.genre_item, linearLayoutGenres, false)
        val textView = genreView.findViewById<TextView>(R.id.textViewGenreName)
        textView.text = name

        genreView.setOnTouchListener { v, event ->
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

    private fun createSubscriptionView(name: String): View {
        val inflater = LayoutInflater.from(this)
        val subscriptionView =
            inflater.inflate(R.layout.subscription_item, linearLayoutSubscriptions, false)
        val textView = subscriptionView.findViewById<TextView>(R.id.textViewSubscriptionName)
        textView.text = name

        subscriptionView.setOnTouchListener { v, event ->
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

        return subscriptionView
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

        // Clear existing checkboxes
        linearLayoutSubscriptions.removeAllViews()
        linearLayoutSubscriptions.setBackgroundResource(R.color.light_blue_600)

        // Add checked subscriptions dynamically as views
        checkedSubscriptions.forEach { subscription ->
            val subscriptionView = createSubscriptionView(subscription)
            linearLayoutSubscriptions.addView(subscriptionView)
        }

        // Set the flag to indicate that the save button has been pressed
        isSaved = true
    }

    private fun handleAddSubscriptionButtonClick() {
        val subscriptionName = editTextAddSubscription.text.toString().trim()
        if (subscriptionName.isNotEmpty()) {
            if (isSaved) {
                // Add as subscription view
                val subscriptionView = createSubscriptionView(subscriptionName)
                linearLayoutSubscriptions.addView(subscriptionView)
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
            val genreView = createGenreView(genreName)
            linearLayoutGenres.addView(genreView)
            editTextAddGenre.text.clear()
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


    private fun getOrderOfItemNames(layout: LinearLayout): List<String> {
        val order = mutableListOf<String>()
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            val textView = view.findViewById<TextView>(R.id.textViewGenreName)
            if (textView != null) {
                order.add(textView.text.toString())
            } else {
                Log.e(
                    "SetupActivity",
                    "TextView with ID textViewGenreName not found in child view at index $i"
                )
            }
        }
        return order
    }

    private fun getOrderOfSubscriptionItemNames(layout: LinearLayout): List<String> {
        val sequence = mutableListOf<String>()
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            val textView = view.findViewById<TextView>(R.id.textViewSubscriptionName)
            if (textView != null) {
                sequence.add(textView.text.toString())
            } else {
                Log.e(
                    "SetupActivity",
                    "TextView with ID textViewGenreName not found in child view at index $i"
                )
            }
        }
        return sequence
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

        val selectedMin = spinnerMin.selectedItem.toString().toInt()
        val selectedMax = spinnerMax.selectedItem.toString().toInt()
        val selectedMinTV = spinnerMinTV.selectedItem.toString().toInt()
        val selectedMaxTV = spinnerMaxTV.selectedItem.toString().toInt()

        val genresToAvoid = selectedGenres.joinToString(", ")
        val subscriptionNames = getOrderOfSubscriptionItemNames(linearLayoutSubscriptions)
        val genreNames = getOrderOfItemNames(linearLayoutGenres)
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
            // Navigate to LoginForm
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
