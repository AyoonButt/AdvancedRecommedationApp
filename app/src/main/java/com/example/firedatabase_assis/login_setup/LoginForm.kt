package com.example.firedatabase_assis.login_setup


import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.databinding.ActivityLoginFormBinding
import com.example.firedatabase_assis.home_page.HomePage

class LoginForm : AppCompatActivity() {
    private lateinit var bind: ActivityLoginFormBinding
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginFormBinding.inflate(layoutInflater)
        setContentView(bind.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE)


        var dbhelp = DB_class(applicationContext)
        var db = dbhelp.readableDatabase

        bind.btnlogin.setOnClickListener {
            val username = bind.logtxt.text.toString()
            val password = bind.ed3.text.toString()
            val query = "SELECT * FROM user WHERE username='$username' AND pswd='$password'"
            val rs = db.rawQuery(query, null)
            if (rs.moveToFirst()) {
                val name = rs.getString(rs.getColumnIndex("name"))
                rs.close()

                // Save the username of the logged-in user to SharedPreferences
                saveLoggedInUser(username)

                startActivity(Intent(this, HomePage::class.java))
            } else {
                val ad = AlertDialog.Builder(this)
                ad.setTitle("Message")
                ad.setMessage("Username or password is incorrect!")
                ad.setPositiveButton("Ok", null)
                ad.show()
            }
        }

        bind.regisLink.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveLoggedInUser(username: String) {
        val editor = sharedPreferences.edit()
        editor.putString("LoggedInUser", username)
        editor.apply()
    }
}
