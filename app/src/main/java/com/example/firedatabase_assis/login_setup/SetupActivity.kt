package com.example.firedatabase_assis.login_setup

import android.content.ClipData
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.utils.SpinnerUtils

class SetupActivity : AppCompatActivity() {

    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerRegion: Spinner
    private lateinit var textViewSelectedOldestDate: TextView
    private lateinit var textViewSelectedMostRecentDate: TextView
    private lateinit var linearLayoutGenres: LinearLayout
    private lateinit var linearLayoutSubscriptions: LinearLayout
    private lateinit var buttonSave: Button
    private lateinit var editTextAddSubscription: EditText
    private lateinit var editTextAddGenre: EditText
    private lateinit var providersRecyclerView: RecyclerView
    private lateinit var providerSearch: ProviderSearch

    private val genres = mutableListOf(
        Genre("Action"), Genre("Comedy"), Genre("Drama"), Genre("Fantasy"), Genre("Horror"),
        Genre("Mystery"), Genre("Thriller"), Genre("Romance")
    )

    private var isSaved = false

    private lateinit var dbHelper: InfoDatabase
    private lateinit var providers: List<Provider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Initialize database helper
        dbHelper = InfoDatabase(applicationContext)

        // Retrieve providers from database
        providers = dbHelper.getAllProviders()

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
        providersRecyclerView = findViewById(R.id.providersRecyclerView)

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

        // Setup RecyclerView and Adapter
        providerSearch = ProviderSearch(providers)
        providersRecyclerView.layoutManager = LinearLayoutManager(this)
        providersRecyclerView.adapter = providerSearch

        // Setup search functionality
        editTextAddSubscription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                providerSearch.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
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
}
