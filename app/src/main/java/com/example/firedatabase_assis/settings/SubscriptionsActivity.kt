package com.example.firedatabase_assis.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.R

class SubscriptionsActivity : AppCompatActivity() {

    private lateinit var btNetflix: CheckBox
    private lateinit var btPrime: CheckBox
    private lateinit var btHBOMax: CheckBox
    private lateinit var btHulu: CheckBox
    private lateinit var btAppleTV: CheckBox
    private lateinit var btPeacock: CheckBox
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscriptions)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        btNetflix = findViewById(R.id.checkBox)
        btPrime = findViewById(R.id.checkBox2)
        btHBOMax = findViewById(R.id.checkBox3)
        btHulu = findViewById(R.id.checkBox4)
        btAppleTV = findViewById(R.id.checkBox5)
        btPeacock = findViewById(R.id.checkBox6)

        // Load saved states of checkboxes
        btNetflix.isChecked = sharedPreferences.getBoolean("Netflix", false)
        btPrime.isChecked = sharedPreferences.getBoolean("Prime", false)
        btHBOMax.isChecked = sharedPreferences.getBoolean("HBOMax", false)
        btHulu.isChecked = sharedPreferences.getBoolean("Hulu", false)
        btAppleTV.isChecked = sharedPreferences.getBoolean("AppleTV", false)
        btPeacock.isChecked = sharedPreferences.getBoolean("Peacock", false)

        val saveSettings = findViewById<Button>(R.id.saveSettings)


        val back_to_main = findViewById<Button>(R.id.backSettings)
        back_to_main.setOnClickListener {
            backtomain(it)
        }
    }


    private fun backtomain(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}
