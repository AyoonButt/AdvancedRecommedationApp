package com.example.firedatabase_assis

//import HomePage
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.firedatabase_assis.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var dbhelp = DB_class(applicationContext)
        var db = dbhelp.writableDatabase

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MovieWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)

        binding.btnrgs.setOnClickListener {
            var name = binding.ed1.text.toString()
            var username = binding.ed2.text.toString()
            var password = binding.ed3.text.toString()

            val passwordPattern =
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[*#&@!$])[a-zA-Z0-9*#&@!$]{8,}\$".toRegex()

            if (name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                if (passwordPattern.matches(password)) {
                    // Password meets all requirements
                    var data = ContentValues()
                    data.put("name", binding.ed1.text.toString())
                    data.put("username", binding.ed2.text.toString())
                    data.put("pswd", binding.ed3.text.toString())
                    var rs: Long = db.insert("user", null, data)
                    if (!rs.equals(-1)) {
                        var ad = AlertDialog.Builder(this)
                        ad.setTitle("Message")
                        ad.setMessage("Account registered successfully")
                        ad.setPositiveButton("Ok", null)
                        ad.show()
                        binding.ed1.text.clear()
                        binding.ed2.text.clear()
                        binding.ed3.text.clear()
                        val intent = Intent(this, SetupActivity::class.java)
                        startActivity(intent)
                    } else {
                        var ad = AlertDialog.Builder(this)
                        ad.setTitle("Message")
                        ad.setMessage("Record not added")
                        ad.setPositiveButton("Ok", null)
                        ad.show()
                        binding.ed1.text.clear()
                        binding.ed2.text.clear()
                        binding.ed3.text.clear()
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
        binding.loginLink.setOnClickListener {
            val intent = Intent(this, login_form::class.java)
            startActivity(intent)
        }

    }

}