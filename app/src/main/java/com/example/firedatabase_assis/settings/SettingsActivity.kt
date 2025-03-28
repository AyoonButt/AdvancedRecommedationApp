package com.example.firedatabase_assis.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivitySettingsBinding
import com.example.firedatabase_assis.interactions.InteractionsActivity
import com.example.firedatabase_assis.login_setup.LoginForm
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.UserUpdate
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {
    private lateinit var bind: ActivitySettingsBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("NightModeSett", Context.MODE_PRIVATE)
        val isNightMode = sharedPreferences.getBoolean("night", false)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        bind = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(bind.root)
        setupBottomNavigation(R.id.bottom_menu_settings)

        ActivityNavigationHelper.setLastOpenedSettingsActivity(this::class.java)

        userViewModel = UserViewModel.getInstance(application)

        // Set username from intent
        userViewModel.currentUser.observe(this) { user ->
            if (user != null) {
                bind.DisplayUsername.text = user.name
                bind.username.text = user.username
                bind.email.text = user.email
            }
        }

        setupToolbar("Settings")

        // Setup clickable layouts
        setupClickListeners()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(bind.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)  // Hide back button
            setDisplayShowHomeEnabled(false)  // Hide home icon
            setDisplayShowTitleEnabled(true)  // Show title
            setTitle(title)  // Sets the title
        }
    }

    private fun setupClickListeners() {
        with(bind) {
            
            findViewById<LinearLayout>(R.id.logoutLayout).setOnClickListener {
                logOut()
            }

            // Edit Profile
            Edituser.setOnClickListener {
                showEditDialog()
            }

            findViewById<MaterialCardView>(R.id.activityLayout).setOnClickListener {
                openUserActivity()
            }

            findViewById<MaterialCardView>(R.id.languageLayout).setOnClickListener {
                openLanguage()
            }

            findViewById<MaterialCardView>(R.id.subscriptionLayout).setOnClickListener {
                openSubscriptions()
            }

            // Handle both Genre and Media Preferences with separate layouts
            findViewById<MaterialCardView>(R.id.genresLayout).setOnClickListener {
                openPreferences()
            }

            findViewById<MaterialCardView>(R.id.preferencesLayout).setOnClickListener {
                preferencesActivity()
            }
        }
    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.edit_user, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Get references to EditTexts and Button
        val editName = dialogView.findViewById<TextInputEditText>(R.id.editName)
        val editUsername = dialogView.findViewById<TextInputEditText>(R.id.editUsername)
        val editEmail = dialogView.findViewById<TextInputEditText>(R.id.editEmail)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.saveButton)

        // Pre-fill current user data
        userViewModel.currentUser.value?.let { user ->
            editName.setText(user.name)
            editUsername.setText(user.username)
            editEmail.setText(user.email)
        }

        // Set up save button click listener
        saveButton.setOnClickListener {
            val name = editName.text.toString().trim()
            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()

            if (validateInput(name, username, email)) {
                lifecycleScope.launch {
                    try {
                        userViewModel.currentUser.value?.let { currentUser ->
                            val updates = UserUpdate(
                                name = name,
                                username = username,
                                email = email
                            )
                            val result =
                                userViewModel.updateUserProfile(currentUser.userId, updates)
                            result.onSuccess { updatedUser ->
                                userViewModel.setUser(updatedUser)
                                dialog.dismiss()
                                Toast.makeText(
                                    this@SettingsActivity,
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { exception ->
                                Toast.makeText(
                                    this@SettingsActivity,
                                    "Failed to update profile: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Error updating profile: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun validateInput(name: String, username: String, email: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun logOut() {
        startActivity(Intent(this, LoginForm::class.java))
    }

    private fun openLanguage() {
        startActivity(Intent(this, LanguageActivity::class.java))
    }

    private fun openPreferences() {
        startActivity(Intent(this, GenresActivity::class.java))
    }

    private fun openSubscriptions() {
        startActivity(Intent(this, SubscriptionsActivity::class.java))
    }

    private fun openUserActivity() {
        startActivity(Intent(this, InteractionsActivity::class.java))
    }

    private fun preferencesActivity() {
        startActivity(Intent(this, PreferencesActivity::class.java))
    }
}
