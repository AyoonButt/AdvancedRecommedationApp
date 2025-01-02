package com.example.firedatabase_assis.home_page

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.Comments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentFragment(private val postId: Int) : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var swipeGestureListener: SwipeGestureListener
    private lateinit var userViewModel: UserViewModel
    private lateinit var fragmentContainerLayout: View
    private lateinit var addCommentButton: Button
    private lateinit var commentInput: EditText
    private var parentCommentId: Int? = null  // Track the parent comment for replies


    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val commentsApi = retrofit.create(Comments::class.java)
    private val fragmentScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)
        commentsRecyclerView = view.findViewById(R.id.comment_recycler_view)
        addCommentButton = view.findViewById(R.id.add_comment_button)
        commentInput = view.findViewById(R.id.comment_input)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())


        // Initialize UserViewModel
        userViewModel = UserViewModel.getInstance(requireActivity().application)

        swipeGestureListener = SwipeGestureListener(requireContext(), fragmentContainerLayout) {
            fragmentContainerLayout.visibility = View.GONE
            requireActivity().supportFragmentManager.popBackStack()
        }

        fragmentContainerLayout.setOnTouchListener { _, event ->
            swipeGestureListener.onTouch(fragmentContainerLayout, event)
        }

        commentsRecyclerView.setOnTouchListener { _, event ->
            swipeGestureListener.onTouch(commentsRecyclerView, event)
        }


        // Set up the adapter with an empty list initially
        commentsAdapter = CommentsAdapter(
            fragmentScope,
            requireContext(),
            listOf(),
            postId,
            userViewModel.getUser()
        ) { parentId ->
            // Set the parentCommentId when a reply button is clicked
            parentCommentId = parentId
            commentInput.requestFocus()  // Focus the input to type the reply
        }

        commentsRecyclerView.adapter = commentsAdapter

        // Handle add comment button click
        addCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        fetchComments()

        return view

    }

    override fun onDestroyView() {
        fragmentScope.cancel()
        super.onDestroyView()
        // Remove the swipe listener when the view is destroyed
        fragmentContainerLayout.setOnTouchListener(null)
        // Remove the layout change listener
        fragmentContainerLayout.viewTreeObserver.removeOnGlobalLayoutListener { }
        Log.d("CommentFragment", "Swipe listener removed")
    }

    private fun getRetrofitInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun addComment(commentText: String) {
        lifecycleScope.launch {
            val currentUser = userViewModel.getUser()


            if (currentUser != null) {
                val newComment = currentUser.userId?.let {
                    CommentDto(
                        commentId = null,
                        userId = it,
                        username = currentUser.username,
                        postId = postId,
                        content = commentText,
                        sentiment = "Neutral",
                        timestamp = System.currentTimeMillis().toString(),
                        parentCommentId = parentCommentId,  // Attach parentCommentId if replying
                    )
                }

                if (newComment != null) {
                    insertComment(newComment)
                }

                // Call the adapter to add the comment
                commentsAdapter.updateComments(listOf(newComment))
                commentInput.text.clear()  // Clear the input field after adding comment
                parentCommentId = null  // Reset parentCommentId after the reply is sent
            } else {
                Toast.makeText(requireContext(), "Failed to add comment", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun refreshComments() {
        lifecycleScope.launch {
            val commentsList = getCommentsForPost(postId)
            commentsAdapter.updateComments(commentsList)
        }
    }

    private suspend fun getCommentsForPost(postId: Int): List<CommentDto> {
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
                    return@withContext emptyList<CommentDto>()
                }
            } catch (e: Exception) {
                // Log error with exception details
                Log.e("API_ERROR", "Error fetching comments for postId: $postId", e)
                return@withContext emptyList<CommentDto>() // Return empty list if there's an error
            }
        }
    }

    private suspend fun insertComment(newComment: CommentDto): Response<String> {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        return api.addComment(newComment)
    }


}