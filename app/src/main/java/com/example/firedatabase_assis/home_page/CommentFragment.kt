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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.ApiResponse
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.Comments
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
    private lateinit var userViewModel: UserViewModel
    private lateinit var commentsViewModel: CommentsViewModel
    private lateinit var addCommentButton: Button
    private lateinit var commentInput: EditText
    private var parentCommentId: Int? = null
    private var isReplyMode = false

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                // Check if fragment is still attached
                activity?.let { activity ->
                    bottomSheet.visibility = View.GONE
                    activity.supportFragmentManager.popBackStack()
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            // Optional: Add slide animations
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commentsViewModel = ViewModelProvider(this)[CommentsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)

        // Initialize views
        commentsRecyclerView = view.findViewById(R.id.comment_recycler_view)
        addCommentButton = view.findViewById(R.id.add_comment_button)
        commentInput = view.findViewById(R.id.comment_input)

        // Set up RecyclerView
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize ViewModel
        userViewModel = UserViewModel.getInstance(requireActivity().application)

        // Set up bottom sheet behavior
        activity?.findViewById<View>(R.id.fragment_container)?.let { container ->
            bottomSheetBehavior = BottomSheetBehavior.from(container).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                isHideable = true
                skipCollapsed = true
                addBottomSheetCallback(bottomSheetCallback)
            }
        }

        // Set up the adapter with reply handler
        commentsAdapter = CommentsAdapter(
            fragmentScope,
            requireContext(),
            mutableListOf(),
            postId,
            userViewModel.getUser(),
            { parentId ->
                // Enhanced reply click handler
                Log.d("CommentFragment", "Reply clicked for parent: $parentId")
                parentCommentId = parentId
                isReplyMode = true
                commentInput.hint = "Write a reply..."
                commentInput.requestFocus()
                showKeyboard()
            },
            isNestedAdapter = false,
            viewModel = commentsViewModel
        )

        // Handle focus changes
        commentInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isReplyMode) {
                // Only reset for new comments, not replies
                parentCommentId = null
            }
        }

        // Handle comment input clicks
        commentInput.setOnClickListener {
            if (!isReplyMode) {
                // Reset only if not in reply mode
                parentCommentId = null
                commentInput.hint = "Write a comment..."
            }
        }

        // Set up add comment button
        addCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                Log.d(
                    "CommentFragment",
                    "Adding comment - isReplyMode: $isReplyMode, parentId: $parentCommentId"
                )
                addComment(commentText)
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Set up RecyclerView adapter
        commentsRecyclerView.adapter = commentsAdapter

        // Fetch initial comments
        fetchComments()

        return view
    }

    private fun showKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        commentInput.post {
            imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }


    override fun onDestroyView() {
        fragmentScope.cancel()
        super.onDestroyView()
        fragmentContainerLayout.setOnTouchListener(null)
        fragmentContainerLayout.viewTreeObserver.removeOnGlobalLayoutListener { }
        Log.d("CommentFragment", "Swipe listener removed")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear ViewModel cache when fragment is destroyed
        if (isRemoving) {
            commentsViewModel.clearCache()
        }
    }


    private fun getRetrofitInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun addComment(commentText: String) {
        lifecycleScope.launch {
            try {
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
                            parentCommentId = if (isReplyMode) parentCommentId else null
                        )
                    }

                    if (newComment != null) {
                        val response = insertComment(newComment)

                        if (response.isSuccessful && response.body()?.success == true) {
                            // Create a new comment with the ID from response if available
                            val commentWithId =
                                if (response.body()?.message?.toIntOrNull() != null) {
                                    newComment.copy(commentId = response.body()?.message?.toIntOrNull())
                                } else {
                                    newComment
                                }

                            withContext(Dispatchers.Main) {
                                if (isReplyMode && parentCommentId != null) {
                                    // Handle reply
                                    val parentComment =
                                        commentsAdapter.getPositionForComment(parentCommentId!!)
                                    if (parentComment != -1) {
                                        // If replies are not visible, make them visible
                                        if (!commentsViewModel.visibleReplySections.value.contains(
                                                parentCommentId
                                            )
                                        ) {
                                            commentsViewModel.toggleReplySection(
                                                parentCommentId!!,
                                                true
                                            )
                                        }

                                        // Add to cache and update UI
                                        commentsViewModel.addCommentToCache(
                                            parentCommentId!!,
                                            commentWithId
                                        )
                                        commentsAdapter.addNewComment(commentWithId)
                                    }
                                } else {
                                    // Add root comment
                                    commentsAdapter.addNewComment(commentWithId)
                                }

                                // Clear input and reset state
                                commentInput.text.clear()
                                parentCommentId = null
                                isReplyMode = false
                                commentInput.hint = "Write a comment..."
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to add comment: ${response.body()?.message ?: "Unknown error"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CommentFragment", "Error adding comment", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error adding comment", Toast.LENGTH_SHORT)
                        .show()
                }
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

    override fun onResume() {
        super.onResume()
        hideBottomNavBar()
    }

    override fun onPause() {
        super.onPause()
        showBottomNavBar()
    }

    private fun hideBottomNavBar() {
        activity?.findViewById<View>(R.id.bottom_nav_bar)?.visibility = View.GONE
    }

    private fun showBottomNavBar() {
        activity?.findViewById<View>(R.id.bottom_nav_bar)?.visibility = View.VISIBLE
    }


}