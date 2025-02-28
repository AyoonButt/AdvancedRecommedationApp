package com.example.firedatabase_assis.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityPreferencesBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.UserEntity
import com.example.firedatabase_assis.postgres.UserUpdate
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesActivity : BaseActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var userViewModel: UserViewModel

    // TV Episode duration ranges (minutes)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)
        ActivityNavigationHelper.setLastOpenedSettingsActivity(this::class.java)


        userViewModel = UserViewModel.getInstance(application)

        setupToolbar("Media Preferences")
        setupDurationSpinners()
        setupDatePickers()
        setupSaveButton()

        // Set initial values from current user
        userViewModel.currentUser.observe(this) { user ->
            user?.let { setInitialValues(it) }
        }
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
            startActivity(intent)
            finish()
        }
    }

    private fun setupDurationSpinners() {
        binding.apply {
            // Movie spinners
            spinnerMinMovie.adapter = createAdapter(minMovieRanges)
            spinnerMaxMovie.adapter = createAdapter(maxMovieRanges)

            // TV spinners
            spinnerMinTv.adapter = createAdapter(minTvRanges)
            spinnerMaxTv.adapter = createAdapter(maxTvRanges)

            // Set listeners to ensure min <= max
            spinnerMinMovie.onItemSelectedListener =
                createMinSpinnerListener(spinnerMaxMovie, minMovieRanges, maxMovieRanges)
            spinnerMinTv.onItemSelectedListener =
                createMinSpinnerListener(spinnerMaxTv, minTvRanges, maxTvRanges)
        }
    }

    private fun createAdapter(ranges: List<Pair<Int, String>>) = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_item,
        ranges.map { it.second }
    ).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    private fun createMinSpinnerListener(
        maxSpinner: Spinner,
        minRanges: List<Pair<Int, String>>,
        maxRanges: List<Pair<Int, String>>
    ) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val minValue = minRanges[position].first
            val maxPosition = maxSpinner.selectedItemPosition
            val maxValue = maxRanges[maxPosition].first

            if (minValue > maxValue) {
                // Find the next valid max position
                val newMaxPosition = maxRanges.indexOfFirst { it.first > minValue }
                if (newMaxPosition != -1) {
                    maxSpinner.setSelection(newMaxPosition)
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    private fun setupDatePickers() {
        binding.apply {
            oldestDatePicker.setOnClickListener {
                showDatePicker("Select Oldest Date") { date ->
                    oldestDatePicker.text = date
                }
            }

            recentDatePicker.setOnClickListener {
                showDatePicker("Select Recent Date") { date ->
                    recentDatePicker.text = date
                }
            }
        }
    }

    private fun showDatePicker(title: String, onDateSelected: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(timestamp))
            onDateSelected(date)
        }

        picker.show(supportFragmentManager, null)
    }

    private fun setInitialValues(user: UserEntity) {
        binding.apply {
            // Set Movie durations
            spinnerMinMovie.setSelection(minMovieRanges.indexOfFirst { it.first == user.minMovie }
                .coerceAtLeast(0))
            spinnerMaxMovie.setSelection(maxMovieRanges.indexOfFirst { it.first == user.maxMovie }
                .coerceAtLeast(0))

            // Set TV durations
            spinnerMinTv.setSelection(minTvRanges.indexOfFirst { it.first == user.minTv }
                .coerceAtLeast(0))
            spinnerMaxTv.setSelection(maxTvRanges.indexOfFirst { it.first == user.maxTv }
                .coerceAtLeast(0))

            // Set dates
            oldestDatePicker.text = user.oldestDate
            recentDatePicker.text = user.recentDate
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    userViewModel.currentUser.value?.let { currentUser ->
                        val updates = UserUpdate(
                            minMovie = minMovieRanges[binding.spinnerMinMovie.selectedItemPosition].first,
                            maxMovie = maxMovieRanges[binding.spinnerMaxMovie.selectedItemPosition].first,
                            minTV = minTvRanges[binding.spinnerMinTv.selectedItemPosition].first,
                            maxTV = maxTvRanges[binding.spinnerMaxTv.selectedItemPosition].first,
                            oldestDate = binding.oldestDatePicker.text.toString(),
                            recentDate = binding.recentDatePicker.text.toString()
                        )

                        val result = userViewModel.updateUserProfile(currentUser.userId, updates)
                        result.onSuccess { updatedUser ->
                            userViewModel.setUser(updatedUser)
                            finish()
                            Toast.makeText(
                                this@PreferencesActivity,
                                "Preferences updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { exception ->
                            Toast.makeText(
                                this@PreferencesActivity,
                                "Failed to update: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@PreferencesActivity,
                        "Error updating preferences: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}