package com.example.firedatabase_assis.login_setup


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
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.PostGenres.avoidGenres
import com.example.firedatabase_assis.database.SubscriptionProviders
import com.example.firedatabase_assis.database.UserGenres
import com.example.firedatabase_assis.database.UserSubscriptions
import com.example.firedatabase_assis.database.Users
import com.example.firedatabase_assis.utils.DataManager
import com.example.firedatabase_assis.utils.DataManager.getGenreIds
import com.example.firedatabase_assis.utils.Genre
import com.example.firedatabase_assis.utils.SpinnerUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
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


    private val selectedGenres = mutableListOf<String>()


    private val genres = mutableListOf(
        Genre(28, "Action"),
        Genre(35, "Comedy"),
        Genre(18, "Drama"),
        Genre(14, "Fantasy"),
        Genre(27, "Horror"),
        Genre(9648, "Mystery"),
        Genre(53, "Thriller"),
        Genre(10749, "Romance")
    )

    private var minValues: List<String> = mutableListOf("0", "1", "2", "3", "4", "5")
    private var maxValues: List<String> = mutableListOf("10", "15", "20", "25", "30")


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
            val genreView = createGenreView(genre.name)
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
            saveSettings()
        }

    }

    private fun filterProviders(query: String) {
        transaction {
            if (query.isNotEmpty()) {
                val filteredProviders = SubscriptionProviders
                    .selectAll()
                    .map {
                        it[SubscriptionProviders.providerName] to lcs(
                            it[SubscriptionProviders.providerName],
                            query
                        )
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }
                    .take(5)

                updateProviders(filteredProviders)
            } else {
                updateProviders(emptyList())
            }
        }
    }


    private fun updateProviders(newProviders: List<String>) {
        linearLayoutProviders.removeAllViews()
        newProviders.forEach { providerName ->
            val providerView = LayoutInflater.from(this)
                .inflate(R.layout.provider_item, linearLayoutProviders, false)
            val textViewProviderName = providerView.findViewById<TextView>(R.id.provider_name)
            textViewProviderName.text = providerName
            providerView.setOnClickListener {
                editTextAddSubscription.setText(providerName)
                linearLayoutProviders.removeAllViews()
            }
            linearLayoutProviders.addView(providerView)
        }
    }


    private fun lcs(str1: String, str2: String): Int {
        val m = str1.length
        val n = str2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (str1[i - 1] == str2[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        return dp[m][n]
    }

    private fun filterGenres(query: String) {
        if (query.isNotEmpty()) {
            val allGenres = DataManager.getGenres()
            val filteredGenres = allGenres
                .map { it to lcs(it.name, query) }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(5)

            updateGenres(filteredGenres)

        } else {
            updateGenres(emptyList())
        }
    }

    private fun filterAvoidGenres(query: String) {
        if (query.isNotEmpty()) {
            val allGenres = DataManager.getGenres()
            val filteredGenres = allGenres
                .map { it to lcs(it.name, query) }
                .sortedByDescending { it.second }
                .map { it.first }
                .take(5)

            updateAvoidGenres(filteredGenres)

        } else {
            updateGenres(emptyList())
        }
    }


    private fun updateGenres(newGenres: List<Genre>) {
        linearLayoutGenresSearch.removeAllViews()
        newGenres.forEach { genre ->
            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutGenresSearch, false)
            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            textViewGenreName.text = genre.name
            genreView.setOnClickListener {
                editTextAddGenre.setText(genre.name)
                linearLayoutGenresSearch.removeAllViews()
            }
            linearLayoutGenresSearch.addView(genreView)
        }
    }

    private fun updateAvoidGenres(newGenres: List<Genre>) {
        linearLayoutAvoidGenres.removeAllViews()
        newGenres.forEach { genre ->
            val genreView = LayoutInflater.from(this)
                .inflate(R.layout.genre_search_item, linearLayoutAvoidGenres, false)
            val textViewGenreName = genreView.findViewById<TextView>(R.id.genres_name)
            textViewGenreName.text = genre.name
            genreView.setOnClickListener {
                editTextAvoidGenres.setText(genre.name)
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

    private fun getProviderIds(names: List<String>): List<Int> {
        return transaction {
            // Query the SubscriptionProviders table
            SubscriptionProviders
                .select { SubscriptionProviders.providerName inList names }
                .map { it[SubscriptionProviders.providerId] }
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

    private fun saveSettings() {
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

        Log.d(
            "SettingsInfo",
            "GenresToAvoid: $genresToAvoid, Subscriptions: $subscriptionIds, Genres: $genreIds"
        )

        // Insert data into the database
        val userId = transaction {
            // Insert the user
            Users.insert {
                it[name] = name_string
                it[username] = username_string
                it[email] = email_string
                it[pswd] = password
                it[language] = languageId
                it[region] = countryId ?: "Default"
                it[minMovie] = selectedMin
                it[maxMovie] = selectedMax
                it[minTV] = selectedMinTV
                it[maxTV] = selectedMaxTV
                it[oldestDate] = selectedOldestDate
                it[recentDate] = selectedMostRecentDate
                it[createdAt] = getCurrentTimestamp()
            }

            // Retrieve the userId of the inserted user
            Users
                .select { Users.email eq email_string } // Assuming email is unique
                .single()[Users.userId]
        }

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("userId", userId)
            apply()
        }


        // Insert user subscriptions
        transaction {
            subscriptionIds.forEach { providerId ->
                UserSubscriptions.insert {
                    it[this.userId] = userId // Use the retrieved userId
                    it[providerID] = providerId
                    it[avoidGenres] = genresToAvoid
                }
            }
        }

        // Insert user genres
        transaction {
            genreIds.forEach { genreId ->
                UserGenres.insert {
                    it[this.userId] = userId // Use the retrieved userId
                    it[genreID] = genreId
                }
            }
        }

        // Navigate to LoginForm
        val intent = Intent(this, LoginForm::class.java)
        startActivity(intent)
        finish()
    }

}
