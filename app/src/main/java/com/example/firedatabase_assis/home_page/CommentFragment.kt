package com.example.firedatabase_assis.home_page

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.CommentEntity
import com.example.firedatabase_assis.postgres.Comments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentFragment(private val postId: Int) : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var swipeGestureListener: SwipeGestureListener
    private lateinit var userViewModel: UserViewModel
    private lateinit var fragmentContainerLayout: View
    private var isLoading = false


    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val commentsApi = retrofit.create(Comments::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)
        commentsRecyclerView = view.findViewById(R.id.comment_recycler_view)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Define the fragment container
        fragmentContainerLayout = requireActivity().findViewById(R.id.fragment_container)

        // Set up swipe gesture listener for the fragment_container layout
        swipeGestureListener = SwipeGestureListener(requireContext(), fragmentContainerLayout) {
            Log.d("SwipeGestureListener", "Swipe down action triggered")
            fragmentContainerLayout.visibility = View.GONE
            requireActivity().supportFragmentManager.popBackStack()
        }

        fragmentContainerLayout.setOnTouchListener { _, event ->
            val result = swipeGestureListener.onTouch(fragmentContainerLayout, event)
            Log.d("CommentFragment", "Touch event: $event, result: $result")
            result
        }

        commentsRecyclerView.setOnTouchListener { _, event ->
            val result = swipeGestureListener.onTouch(commentsRecyclerView, event)
            Log.d("CommentFragment", "RecyclerView touch event: $event, result: $result")
            result
        }

        userViewModel = UserViewModel.getInstance(requireActivity().application)

        // Initialize the CommentsAdapter with the callback
        commentsAdapter = CommentsAdapter(
            requireContext(),
            listOf(),
            postId,
            userViewModel.getUser()
        ) {
            // This will be called when a comment is added
            refreshComments()
        }


        commentsRecyclerView.adapter = commentsAdapter


        Log.d("CommentFragment", "Adapter set to RecyclerView")

        Log.d("CommentFragment", "User passed to CommentsAdapter: ${userViewModel.getUser()}")

        fetchComments()

        return view
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the swipe listener when the view is destroyed
        fragmentContainerLayout.setOnTouchListener(null)
        // Remove the layout change listener
        fragmentContainerLayout.viewTreeObserver.removeOnGlobalLayoutListener { }
        Log.d("CommentFragment", "Swipe listener removed")
    }

    fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return fragmentContainerLayout.dispatchTouchEvent(event)
    }

    private fun fetchComments() {
        Log.d("CommentFragment", "Fetching comments for postId: $postId")

        lifecycleScope.launch {
            try {
                // Fetch comments for the postId
                val commentsList = getCommentsForPost(postId)

                // Log the fetched comments
                if (commentsList.isNotEmpty()) {
                    Log.d("CommentFragment", "Successfully fetched ${commentsList.size} comments")
                } else {
                    Log.d("CommentFragment", "No comments found for postId: $postId")
                }

                // Update the adapter with the fetched comments
                commentsAdapter.updateComments(commentsList)
                Log.d("CommentFragment", "Adapter updated with new comments")
            } catch (e: Exception) {
                // Log any errors that occur
                Log.e("CommentFragment", "Error fetching comments: ${e.message}")
            }
        }
    }


    // In CommentFragment
    private fun refreshComments() {
        lifecycleScope.launch {
            val commentsList = getCommentsForPost(postId)
            commentsAdapter.updateComments(commentsList)
        }
    }

    private suspend fun getCommentsForPost(postId: Int): List<CommentEntity> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch comments for the given postId
                val response = commentsApi.getCommentsByPost(postId)

                // Check if response is successful
                if (response.isSuccessful) {
                    return@withContext response.body()
                        ?: emptyList() // Return empty list if no comments found
                } else {
                    // Log unsuccessful response status code
                    Log.e(
                        "API_ERROR",
                        "Failed to fetch comments for postId: $postId, Response code: ${response.code()}"
                    )
                    return@withContext emptyList<CommentEntity>()
                }
            } catch (e: Exception) {
                // Log error with exception details
                Log.e("API_ERROR", "Error fetching comments for postId: $postId", e)
                return@withContext emptyList<CommentEntity>() // Return empty list if there's an error
            }
        }
    }
}