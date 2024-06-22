package com.example.firedatabase_assis


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.databinding.ActivityUserBinding
import com.example.firedatabase_assis.login_setup.DB_class


class UserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserBinding
    private lateinit var dbHelper: DB_class
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var dbhelp = DB_class(applicationContext)
        var db = dbhelp.readableDatabase
        var name = binding.Edituser.text.toString()
        val query = "SELECT * FROM user WHERE username='$name'"
        val username = intent.getStringExtra("name")

        binding.Edituser.text = username


//        dbhelp = DB_class(applicationContext)
//
//        Edituser = findViewById<EditText>(R.id.Edituser)
//        authFire = Authenticator.getInstance()


//        userName = intent.getStringExtra("name") ?: "name"
//
//        bind.Edituser.setText(userName)
//
//        bind.btUpdateUser.setOnClickListener{
//            val newName = bind.Edituser.text.toString()
//            updateNameInDatabase(newName)
//            userName = newName
//        }
//    }
//
//    private fun updateNameInDatabase(newName: String){
//        val db = dbhelp.writableDatabase
//        val values = ContentValues().apply {
//            put(DB_class.KEY_NAME, newName)
//        }
//        db.update(DB_class.TABLE_CONTACTS, values, null, null)
//        db.close()
//    }
    }
}