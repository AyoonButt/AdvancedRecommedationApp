package com.example.firedatabase_assis


import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.databinding.ActivityUserBinding
import com.example.firedatabase_assis.databinding.ActivityWelcomeWindowBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.net.Authenticator


class UserActivity : AppCompatActivity(){
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
}}