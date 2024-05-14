package com.example.firedatabase_assis

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private lateinit var btNetflix: CheckBox
    private lateinit var btPrime: CheckBox
    private lateinit var btHBOMax: CheckBox
    private lateinit var btHulu: CheckBox
    private lateinit var btAppleTV: CheckBox
    private lateinit var btPeacock: CheckBox

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
        val scrollView = ScrollView(this)
        scrollView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val inflater = layoutInflater
        val container = inflater.inflate(R.layout.activity_setup, null) as ViewGroup
        scrollView.addView(container)
        setContentView(scrollView)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Initialize checkboxes for subscriptions
        btNetflix = findViewById(R.id.checkBoxNetflix)
        btPrime = findViewById(R.id.checkBoxPrime)
        btHBOMax = findViewById(R.id.checkBoxHBOMax)
        btHulu = findViewById(R.id.checkBoxHulu)
        btAppleTV = findViewById(R.id.checkBoxAppleTV)
        btPeacock = findViewById(R.id.checkBoxPeacock)

        // Initialize checkboxes for genres
        btAction = findViewById(R.id.checkBox)
        btComedy = findViewById(R.id.checkBox2)
        btDrama = findViewById(R.id.checkBox3)
        btFantasy = findViewById(R.id.checkBox4)
        btHorror = findViewById(R.id.checkBox5)
        btMystery = findViewById(R.id.checkBox6)
        btThriller = findViewById(R.id.checkBox7)
        btRomance = findViewById(R.id.checkBox8)


        // Load saved states of checkboxes for subscriptions
        btNetflix.isChecked = sharedPreferences.getBoolean("Netflix", false)
        btPrime.isChecked = sharedPreferences.getBoolean("Prime", false)
        btHBOMax.isChecked = sharedPreferences.getBoolean("HBOMax", false)
        btHulu.isChecked = sharedPreferences.getBoolean("Hulu", false)
        btAppleTV.isChecked = sharedPreferences.getBoolean("AppleTV", false)
        btPeacock.isChecked = sharedPreferences.getBoolean("Peacock", false)

        // Load saved states of checkboxes for genres
        btAction.isChecked = sharedPreferences.getBoolean("Action", false)
        btComedy.isChecked = sharedPreferences.getBoolean("Comedy", false)
        btDrama.isChecked = sharedPreferences.getBoolean("Drama", false)
        btFantasy.isChecked = sharedPreferences.getBoolean("Fantasy", false)
        btHorror.isChecked = sharedPreferences.getBoolean("Horror", false)
        btMystery.isChecked = sharedPreferences.getBoolean("Mystery", false)
        btThriller.isChecked = sharedPreferences.getBoolean("Thriller", false)
        btRomance.isChecked = sharedPreferences.getBoolean("Romance", false)

        val saveAndContinue = findViewById<Button>(R.id.saveSettings)
        saveAndContinue.setOnClickListener {
            saveStates()
            updateDatabase()
            launchSetup(it)

        }

    }

    private fun saveStates() {
        val editor = sharedPreferences.edit()

        // Save states of checkboxes for subscriptions
        editor.putBoolean("Netflix", btNetflix.isChecked)
        editor.putBoolean("Prime", btPrime.isChecked)
        editor.putBoolean("HBOMax", btHBOMax.isChecked)
        editor.putBoolean("Hulu", btHulu.isChecked)
        editor.putBoolean("AppleTV", btAppleTV.isChecked)
        editor.putBoolean("Peacock", btPeacock.isChecked)

        // Save states of checkboxes for genres
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
        val selectedServices = mutableListOf<String>()
        if (btNetflix.isChecked) selectedServices.add("Netflix")
        if (btPrime.isChecked) selectedServices.add("Prime")
        if (btHBOMax.isChecked) selectedServices.add("HBOMax")
        if (btHulu.isChecked) selectedServices.add("Hulu")
        if (btAppleTV.isChecked) selectedServices.add("AppleTV")
        if (btPeacock.isChecked) selectedServices.add("Peacock")

        dbhelper.updateServicesList(username, selectedServices)

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

    private fun launchSetup(view: View) {
        val intent = Intent(this, StreamingAPI::class.java)
        startActivity(intent)
    }


}
