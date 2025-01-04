package com.example.firedatabase_assis.home_page

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.example.firedatabase_assis.postgres.ApiResponse
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
    private lateinit var addCommentButton: Button
    private lateinit var commentInput: EditText
    private var parentCommentId: Int? = null

    private val fragmentContainerLayout by lazy {
        requireActivity().findViewById<View>(R.id.fragment_container)
    }

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

        // Initialize swipe gesture after fragmentContainerLayout is initialized
        view.post {
            // Initialize swipe gesture listener with the current fragment container
            swipeGestureListener = SwipeGestureListener(
                requireContext(),
                fragmentContainerLayout
            ) {
                fragmentContainerLayout.visibility = View.GONE
                requireActivity().supportFragmentManager.popBackStack()
            }

            // Create a custom touch interceptor for the RecyclerView that checks scroll position
            val recyclerViewTouchInterceptor = object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    // Only allow swipe gesture if we're at the top
                    val isAtTop = !rv.canScrollVertically(-1)
                    return if (isAtTop) {
                        swipeGestureListener.onTouch(rv, e)
                    } else {
                        false // Let the RecyclerView handle the touch event for scrolling
                    }
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                    // No additional handling needed
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    // No additional handling needed
                }
            }

            // Add the touch interceptor to the RecyclerView
            commentsRecyclerView.addOnItemTouchListener(recyclerViewTouchInterceptor)

            // Handle fragment container touch events
            fragmentContainerLayout.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Let the gesture listener know about the initial touch
                        swipeGestureListener.onTouch(v, event)
                    }

                    MotionEvent.ACTION_UP -> {
                        v.performClick()
                    }
                }
                // Always allow swipe gesture on the fragment container
                swipeGestureListener.onTouch(v, event)
            }
            fragmentContainerLayout.isClickable = true
            fragmentContainerLayout.isFocusable = true
        }
        // Set up the adapter
        commentsAdapter = CommentsAdapter(
            fragmentScope,
            requireContext(),
            mutableListOf(),
            postId,
            userViewModel.getUser(),
            { parentId ->
                parentCommentId = parentId
                commentInput.requestFocus()
            },
            isNestedAdapter = false  // First level comments
        )

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
        fragmentContainerLayout.setOnTouchListener(null)
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
                refreshComments()
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

    private suspend fun insertComment(newComment: CommentDto): Response<ApiResponse> {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        return api.addComment(newComment)
    }

    fun showKeyboardForReply(parentCommentId: Int) {
        commentInput.requestFocus()
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT)
        // Optionally scroll to the input
        commentsRecyclerView.postDelayed({
            (commentsRecyclerView.layoutManager as LinearLayoutManager).scrollToPosition(
                commentsRecyclerView.adapter?.itemCount ?: 0
            )
        }, 100)
    }


}