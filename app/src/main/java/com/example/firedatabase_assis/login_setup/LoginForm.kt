package com.example.firedatabase_assis.login_setup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.database.Users
import com.example.firedatabase_assis.database.connectToDatabase
import com.example.firedatabase_assis.database.createTables
import com.example.firedatabase_assis.databinding.ActivityLoginFormBinding
import com.example.firedatabase_assis.home_page.HomePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction


class LoginForm : AppCompatActivity() {
    private lateinit var bind: ActivityLoginFormBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginFormBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // Initialize the database connection
        connectToDatabase()
        createTables()

        bind.btnlogin.setOnClickListener {
            val username = bind.logtxt.text.toString()
            val password = bind.ed3.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val userExists = checkUserCredentials(username, password)
                withContext(Dispatchers.Main) {
                    if (userExists) {
                        // Pass the username to the HomePage activity
                        val intent = Intent(this@LoginForm, HomePage::class.java).apply {
                            putExtra("username", username)
                        }
                        startActivity(intent)
                    } else {
                        val ad = AlertDialog.Builder(this@LoginForm)
                        ad.setTitle("Message")
                        ad.setMessage("Username or password is incorrect!")
                        ad.setPositiveButton("Ok", null)
                        ad.show()
                    }
                }
            }
        }

        bind.regisLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun checkUserCredentials(username: String, password: String): Boolean {
        return transaction {
            Users.select {
                (Users.username eq username) and (Users.pswd eq password)
            }.singleOrNull() != null
        }
    }
}

