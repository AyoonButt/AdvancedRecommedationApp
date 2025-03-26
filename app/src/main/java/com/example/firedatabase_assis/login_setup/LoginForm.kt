package com.example.firedatabase_assis.login_setup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.databinding.ActivityLoginFormBinding
import com.example.firedatabase_assis.home_page.HomePage
import com.example.firedatabase_assis.postgres.Users
import com.example.firedatabase_assis.workers.ApiWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginForm : AppCompatActivity() {
    private lateinit var bind: ActivityLoginFormBinding

    // Instantiate the UserViewModel
    private lateinit var userViewModel: UserViewModel

    // Retrofit instance for the Users interface
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val usersApi = retrofit.create(Users::class.java)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginFormBinding.inflate(layoutInflater)
        setContentView(bind.root)
        WorkManager.getInstance(this).cancelAllWork()

        userViewModel = UserViewModel.getInstance(application)

        bind.btnlogin.setOnClickListener {
            val username = bind.logtxt.text.toString()
            val password = bind.ed3.text.toString()

            CoroutineScope(Dispatchers.IO).launch {
                val userExists = checkUserCredentials(username, password)
                withContext(Dispatchers.Main) {
                    if (userExists) {
                        // Fetch the user details
                        val userResponse = usersApi.getUserByUsername(username)
                        if (userResponse.isSuccessful) {
                            userResponse.body()?.let { userEntity ->
                                // Update UserViewModel with the user entity
                                val timestamp = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                updateRecentLogin(username, timestamp)

                                userViewModel.setUser(userEntity)
                                Log.d("LogIn", "User Entity $userEntity")

                                setupWorkManager()
                                val intent = Intent(this@LoginForm, HomePage::class.java).apply {
                                    putExtra("username", username)
                                    putExtra("is_navigation", false)
                                }
                                startActivity(intent)
                            }
                        } else {
                            showErrorDialog("Failed to retrieve user information.")
                        }
                    } else {
                        showErrorDialog("Username or password is incorrect!")
                    }
                }
            }
        }

        bind.regisLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        
    }


    private fun setupWorkManager() {
        // Get userId and language from UserViewModel
        val userId = userViewModel.getUserId()
        val language = userViewModel.getLanguage()

        // Log the userId and language for debugging
        Log.d(
            "LoginForm",
            "Setting up WorkManager with userId: ${userId ?: -1}, language: ${language ?: "en"}"
        )

        // Create the input data to pass to the worker
        val inputData = workDataOf(
            "userId" to (userId
                ?: -1),  // Ensure userId is not null, provide default value if necessary
            "language" to (language ?: "en")  // Provide a default language if necessary
        )

        // Set constraints for the worker (if needed)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // Example constraint (requires network)
            .build()

        // Log the constraints for debugging
        Log.d("LoginForm", "Worker constraints set: NetworkType.CONNECTED")

        // Create the worker request with input data and constraints
        val workerRequest = OneTimeWorkRequest.Builder(ApiWorker::class.java)
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        // Log worker creation details
        Log.d("LoginForm", "Worker request created for ApiWorker with inputData: $inputData")

        // Use enqueueUniqueWork to ensure only one instance runs at a time
        try {
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "ApiWorkerUnique", // Unique name for the worker
                ExistingWorkPolicy.REPLACE, // Replace any ongoing or queued work with the same name
                workerRequest
            )
            Log.i("LoginForm", "Worker enqueued successfully with userId: ${userId ?: -1}")
        } catch (e: Exception) {
            Log.e("LoginForm", "Error enqueuing worker: ${e.message}")
        }
    }


    private suspend fun checkUserCredentials(username: String, password: String): Boolean {
        val response = usersApi.checkUserCredentials(username, password)
        return response.isSuccessful && response.body() == true
    }

    private suspend fun updateRecentLogin(username: String, timestamp: String) {
        usersApi.updateRecentLogin(username, timestamp)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this@LoginForm)
            .setTitle("Message")
            .setMessage(message)
            .setPositiveButton("Ok", null)
            .show()
    }
}