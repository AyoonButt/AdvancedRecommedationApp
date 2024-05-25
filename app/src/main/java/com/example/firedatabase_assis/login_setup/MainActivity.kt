package com.example.firedatabase_assis.login_setup


//import HomePage

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.firedatabase_assis.DB_class
import com.example.firedatabase_assis.databinding.ActivityMainBinding
import com.example.firedatabase_assis.workers.DisneyWorker
import com.example.firedatabase_assis.workers.PrimeWorker
import com.jakewharton.threetenabp.AndroidThreeTen


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbhelp = DB_class(applicationContext)
        val db = dbhelp.writableDatabase

        setupWorkManager()

        binding.btnrgs.setOnClickListener {
            handleRegistration(db)
        }

        binding.loginLink.setOnClickListener {
            val intent = Intent(this, login_form::class.java)
            startActivity(intent)
        }
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val primeWorkerRequest = OneTimeWorkRequest.Builder(PrimeWorker::class.java)
            .setConstraints(constraints)
            .build()

        val disneyWorkerRequest = OneTimeWorkRequest.Builder(DisneyWorker::class.java)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(primeWorkerRequest)
        WorkManager.getInstance(applicationContext).enqueue(disneyWorkerRequest)
    }

    private fun handleRegistration(db: SQLiteDatabase) {
        val name = binding.ed1.text.toString()
        val username = binding.ed2.text.toString()
        val password = binding.ed3.text.toString()

        val passwordPattern =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[*#&@!$])[a-zA-Z0-9*#&@!$]{8,}\$".toRegex()

        if (name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            if (passwordPattern.matches(password)) {
                val data = ContentValues().apply {
                    put("name", name)
                    put("username", username)
                    put("pswd", password)
                }
                val rs: Long = db.insert("user", null, data)
                if (rs != -1L) {
                    AlertDialog.Builder(this)
                        .setTitle("Message")
                        .setMessage("Account registered successfully")
                        .setPositiveButton("Ok", null)
                        .show()
                    binding.ed1.text.clear()
                    binding.ed2.text.clear()
                    binding.ed3.text.clear()
                    val intent = Intent(this, SetupActivity::class.java)
                    startActivity(intent)
                } else {
                    showAlert("Record not added")
                }
            } else {
                Toast.makeText(
                    this,
                    "Password must contain at least one lowercase letter, one uppercase letter, one number, and one special character (*#&@!$), and must be at least 8 characters long",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Message")
            .setMessage(message)
            .setPositiveButton("Ok", null)
            .show()
        binding.ed1.text.clear()
        binding.ed2.text.clear()
        binding.ed3.text.clear()
    }
}
