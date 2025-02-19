package com.example.firedatabase_assis.settings

import SpinnerUtils
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityLanguageBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.UserEntity
import com.example.firedatabase_assis.postgres.UserUpdate
import kotlinx.coroutines.launch

class LanguageActivity : BaseActivity() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)

        userViewModel = UserViewModel.getInstance(application)

        setupToolbar("Language and Region")
        setupLanguageSpinner()
        setupRegionSpinner()
        setupSaveButton()

        userViewModel.currentUser.observe(this) { user ->
            user?.let {
                setInitialValues(it)
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

    private fun setupLanguageSpinner() {
        SpinnerUtils.setupLanguageSpinner(this, binding.spinnerLanguage)
    }

    private fun setupRegionSpinner() {
        SpinnerUtils.setupRegionSpinner(this, binding.spinnerRegion)
    }


    private fun setInitialValues(user: UserEntity) {
        // Get language position
        val languagePosition = SpinnerUtils.getLanguagePosition(user.language)
        binding.spinnerLanguage.setSelection(languagePosition)

        // Get region position
        val regionPosition = SpinnerUtils.getRegionPosition(user.region)
        binding.spinnerRegion.setSelection(regionPosition)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (binding.spinnerLanguage.selectedItemPosition == 0 ||
                binding.spinnerRegion.selectedItemPosition == 0
            ) {
                Toast.makeText(
                    this,
                    "Please select both language and region",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    userViewModel.currentUser.value?.let { currentUser ->
                        val selectedLanguageFullName =
                            binding.spinnerLanguage.selectedItem.toString()
                        val selectedRegionFullName = binding.spinnerRegion.selectedItem.toString()

                        val languageCode = SpinnerUtils.getLanguageCode(selectedLanguageFullName)
                        val regionCode = SpinnerUtils.getRegionCode(selectedRegionFullName)

                        val updates = UserUpdate(
                            language = languageCode,
                            region = regionCode
                        )

                        val result = userViewModel.updateUserProfile(currentUser.userId, updates)
                        result.onSuccess { updatedUser ->
                            userViewModel.setUser(updatedUser)
                            finish()
                            Toast.makeText(
                                this@LanguageActivity,
                                "Language and region updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { exception ->
                            Toast.makeText(
                                this@LanguageActivity,
                                "Failed to update: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LanguageActivity,
                        "Error updating settings: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}