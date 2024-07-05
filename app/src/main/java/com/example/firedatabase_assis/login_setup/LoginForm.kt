package com.example.firedatabase_assis.login_setup

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.databinding.ActivityLoginFormBinding
import com.example.firedatabase_assis.home_page.HomePage

class LoginForm : AppCompatActivity() {
    private lateinit var bind: ActivityLoginFormBinding

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginFormBinding.inflate(layoutInflater)
        setContentView(bind.root)

        val dbhelp = DB_class(applicationContext)
        val db = dbhelp.readableDatabase

        bind.btnlogin.setOnClickListener {
            val username = bind.logtxt.text.toString()
            val password = bind.ed3.text.toString()
            val query = "SELECT * FROM users WHERE username='$username' AND pswd='$password'"
            val rs = db.rawQuery(query, null)
            if (rs.moveToFirst()) {
                rs.close()

                // Pass the username to the HomePage activity
                val intent = Intent(this, HomePage::class.java).apply {
                    putExtra("username", username)
                }
                startActivity(intent)
            } else {
                val ad = AlertDialog.Builder(this)
                ad.setTitle("Message")
                ad.setMessage("Username or password is incorrect!")
                ad.setPositiveButton("Ok", null)
                ad.show()
            }
        }

        bind.regisLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}

