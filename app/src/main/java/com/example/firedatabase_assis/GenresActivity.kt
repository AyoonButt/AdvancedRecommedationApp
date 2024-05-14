package com.example.firedatabase_assis

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity

class GenresActivity : AppCompatActivity() {

    private lateinit var btAction: CheckBox
    private lateinit var btComedy: CheckBox
    private lateinit var btDrama: CheckBox
    private lateinit var btFantasy: CheckBox
    private lateinit var btHorror: CheckBox
    private lateinit var btMystery: CheckBox
    private lateinit var btThriller: CheckBox
    private lateinit var btRomance: CheckBox
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        btAction = findViewById(R.id.checkBox)
        btComedy = findViewById(R.id.checkBox2)
        btDrama = findViewById(R.id.checkBox3)
        btFantasy = findViewById(R.id.checkBox4)
        btHorror = findViewById(R.id.checkBox5)
        btMystery = findViewById(R.id.checkBox6)
        btThriller = findViewById(R.id.checkBox7)
        btRomance = findViewById(R.id.checkBox8)

        // Load saved states of checkboxes
        btAction.isChecked = sharedPreferences.getBoolean("Action", false)
        btComedy.isChecked = sharedPreferences.getBoolean("Comedy", false)
        btDrama.isChecked = sharedPreferences.getBoolean("Drama", false)
        btFantasy.isChecked = sharedPreferences.getBoolean("Fantasy", false)
        btHorror.isChecked = sharedPreferences.getBoolean("Horror", false)
        btMystery.isChecked = sharedPreferences.getBoolean("Mystery", false)
        btThriller.isChecked = sharedPreferences.getBoolean("Thriller", false)
        btRomance.isChecked = sharedPreferences.getBoolean("Romance", false)

        val saveSettings = findViewById<Button>(R.id.saveSettings)
        saveSettings.setOnClickListener {
            saveStates()
            updateDatabase()
        }

        val back_to_main = findViewById<Button>(R.id.backSettings)
        back_to_main.setOnClickListener {
            backtomain(it)
        }
    }

    private fun saveStates() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("Action", btAction.isChecked)
        editor.putBoolean("Comedy", btComedy.isChecked)
        editor.putBoolean("Drama", btDrama.isChecked)
        editor.putBoolean("Fantasy", btFantasy.isChecked)
        editor.putBoolean("Horror", btHorror.isChecked)
        editor.putBoolean("Mystery", btMystery.isChecked)
        editor.putBoolean("Thriller", btThriller.isChecked)
        editor.putBoolean("Romance", btRomance.isChecked)
        editor.apply()
    }

    private fun updateDatabase() {
        val username = getLoggedInUser()
        if (username.isNullOrEmpty()) {
            // No logged-in user, cannot update database
            return
        }

        val dbhelper = DB_class(applicationContext)
        val selectedGenres = mutableListOf<String>()
        if (btAction.isChecked) selectedGenres.add("Action")
        if (btComedy.isChecked) selectedGenres.add("Comedy")
        if (btDrama.isChecked) selectedGenres.add("Drama")
        if (btFantasy.isChecked) selectedGenres.add("Fantasy")
        if (btHorror.isChecked) selectedGenres.add("Horror")
        if (btMystery.isChecked) selectedGenres.add("Mystery")
        if (btThriller.isChecked) selectedGenres.add("Thriller")
        if (btRomance.isChecked) selectedGenres.add("Romance")

        dbhelper.updateGenresList(username, selectedGenres)

    }

    private fun getLoggedInUser(): String? {
        return sharedPreferences.getString("LoggedInUser", null)
    }

    private fun backtomain(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}
