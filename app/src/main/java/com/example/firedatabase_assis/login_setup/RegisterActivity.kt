package com.example.firedatabase_assis.login_setup


//import HomePage
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.databinding.ActivityMainBinding
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnrgs.setOnClickListener {
            handleRegistration()
        }

        binding.loginLink.setOnClickListener {
            val intent = Intent(this, LoginForm::class.java)
            startActivity(intent)
        }
    }

    private fun handleRegistration() {
        val name = binding.ed1.text.toString()
        val username = binding.ed2.text.toString()
        val email = binding.ed3.text.toString()
        val password = binding.ed4.text.toString()

        val passwordPattern =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[*#&@!$])[a-zA-Z0-9*#&@!$]{8,}\$".toRegex()

        if (name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            if (passwordPattern.matches(password)) {
                // Start the async initialization of genres and providers
                CoroutineScope(Dispatchers.Main).launch {
                    try {

                        // Once initialized, proceed to SetupActivity
                        val intent =
                            Intent(this@RegisterActivity, SetupActivity::class.java).apply {
                                putExtra("name", name)
                                putExtra("username", username)
                                putExtra("email", email)
                                putExtra("password", password)
                            }
                        startActivity(intent)

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Error initializing data. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
}
