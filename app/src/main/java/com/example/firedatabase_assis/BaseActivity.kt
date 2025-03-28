package com.example.firedatabase_assis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.firedatabase_assis.databinding.ActivityBaseBinding
import com.example.firedatabase_assis.explore.LoadVideos
import com.example.firedatabase_assis.home_page.HomePage
import com.example.firedatabase_assis.search.SearchActivity
import com.example.firedatabase_assis.settings.ActivityNavigationHelper
import com.example.firedatabase_assis.settings.SettingsActivity

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var baseBinding: ActivityBaseBinding  // Made protected for subclasses to access

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNightMode()
    }

    override fun setContentView(layoutResId: Int) {
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        layoutInflater.inflate(layoutResId, baseBinding.contentContainer, true)
        super.setContentView(baseBinding.root)
    }

    override fun setContentView(view: View) {
        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        baseBinding.contentContainer.addView(view)
        super.setContentView(baseBinding.root)
    }

    private fun setupNightMode() {
        val sharedPreferences = getSharedPreferences("NightModeSett", Context.MODE_PRIVATE)
        val isNightMode = sharedPreferences.getBoolean("night", false)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    protected fun setupBottomNavigation(selectedItemId: Int) {
        baseBinding.bottomNavBar.selectedItemId = selectedItemId
        baseBinding.bottomNavBar.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_menu_home -> {
                    if (menuItem.itemId != selectedItemId) {
                        // Moving to Home from different tab - reuse existing
                        val intent = Intent(this, HomePage::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            // Add extra to indicate this is a navigation action, not a refresh request
                            putExtra("is_navigation", true)
                        }
                        startActivity(intent)
                    } else {
                        // Already on Home - refresh
                        if (this is HomePage) {
                            refreshData()
                        } else {
                            startActivity(Intent(this, HomePage::class.java))
                            finish()
                        }
                    }
                }

                R.id.bottom_menu_explore -> {
                    if (menuItem.itemId != selectedItemId) {
                        val intent = Intent(this, LoadVideos::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            // Add extra to indicate this is a navigation action
                            putExtra("is_navigation", true)
                        }
                        startActivity(intent)
                    } else {
                        if (this is LoadVideos) {
                            refreshVideoData()
                        } else {
                            startActivity(Intent(this, LoadVideos::class.java))
                            finish()
                        }
                    }
                }

                R.id.bottom_menu_search -> {
                    if (menuItem.itemId != selectedItemId) {
                        // Moving to Search from different tab
                        val intent = Intent(this, SearchActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        startActivity(intent)
                    } else {
                        // Refreshing Search tab
                        val intent = Intent(this, SearchActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("refresh", true)  // Add flag to indicate refresh
                        }
                        startActivity(intent)
                    }
                }

                R.id.bottom_menu_settings -> {
                    // Check if we're in a settings-related activity
                    val isInSettingsActivity = this::class.java == SettingsActivity::class.java
                    val isInOtherSettingsActivity =
                        this::class.java.name.contains("settings", ignoreCase = true)
                    val isInInteractionsPackage =
                        this::class.java.name.contains("interactions", ignoreCase = true)

                    // If in a settings-related activity but not the main one, go back to main settings
                    if (!isInSettingsActivity && (isInOtherSettingsActivity || isInInteractionsPackage)) {
                        val intent = Intent(this, SettingsActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            putExtra("is_navigation", true)
                        }
                        startActivity(intent)
                    } else if (isInSettingsActivity) {
                        // Already on main Settings - do nothing or perhaps refresh
                    } else {
                        // Not in any settings activity, use the last visited logic
                        val lastActivity = ActivityNavigationHelper.getLastOpenedSettingsActivity()

                        if (lastActivity != null && lastActivity != SettingsActivity::class.java) {
                            val intent = Intent(this, lastActivity).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                putExtra("is_navigation", true)
                            }
                            startActivity(intent)
                        } else {
                            // Default to main SettingsActivity
                            val intent = Intent(this, SettingsActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                putExtra("is_navigation", true)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
            menuItem.itemId == selectedItemId
        }
    }
}