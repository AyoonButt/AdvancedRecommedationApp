package com.example.firedatabase_assis

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReviewActivity : AppCompatActivity() {

    private lateinit var movieRating: RatingBar
    private lateinit var userReview: EditText
    private lateinit var reviewDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        userReview = findViewById(R.id.user_review)
        movieRating = findViewById(R.id.movie_rating_bar)
        reviewDisplay =
            findViewById(R.id.review_display)  // Reference to the review display TextView

        val submitButton = findViewById<Button>(R.id.movie_submit)
        submitButton.setOnClickListener {
            val rating = movieRating.rating
            val reviewText = userReview.text.toString()
            if (reviewText.isNotEmpty()) {
                val newReview = "Rating: $rating\nReview: $reviewText\n\n"
                reviewDisplay.append(newReview)  // Append the new review to the existing text
                userReview.text.clear()  // Clear the EditText after submission
                movieRating.rating = 0f  // Reset the rating bar
            }
        }
    }
}
