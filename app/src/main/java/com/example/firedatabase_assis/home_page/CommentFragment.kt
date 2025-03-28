package com.example.firedatabase_assis.home_page


import android.content.Context
import android.graphics.Rect
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
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.CommentResponse
import com.example.firedatabase_assis.postgres.Comments
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentFragment(private val postId: Int, private val commentType: String) : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var userViewModel: UserViewModel
    private lateinit var commentsViewModel: CommentsViewModel
    private lateinit var addCommentButton: Button
    private lateinit var commentInput: EditText
    private var parentCommentId: Int? = null
    private var isReplyMode = false
    private lateinit var webSocketManager: CommentWebSocketManager

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

    private var readyCallback: (() -> Unit)? = null

    fun onReady(callback: () -> Unit) {
        if (::commentsAdapter.isInitialized) {
            callback()
        } else {
            readyCallback = callback
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

        webSocketManager = CommentWebSocketManager(
            BuildConfig.POSTRGRES_API_URL,
            commentsViewModel,
            fragmentScope
        )

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

        commentsAdapter = CommentsAdapter(
            fragmentScope,
            requireContext(),
            mutableListOf(),
            { parentId ->
                Log.d("CommentFragment", "Reply clicked with parentId: $parentId")  // Add this
                parentCommentId = parentId
                isReplyMode = true
                commentInput.hint = "Write a reply..."
                commentInput.requestFocus()
                showKeyboard()
            },
            isNestedAdapter = false,
            viewModel = commentsViewModel
        )

        readyCallback?.let {
            it()
            readyCallback = null
        }

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

    private fun toggleReplySection(commentId: Int, show: Boolean) {
        Log.d(
            "CommentFragment",
            "Attempting to toggle reply section for comment $commentId, show: $show"
        )

        // Then wait for next frame to ensure views are laid out
        commentsRecyclerView.post {
            val position = commentsAdapter.getPositionForComment(commentId)
            Log.d("CommentFragment", "Position for comment $commentId: $position")

            if (position != -1) {
                val comment = commentsAdapter.commentsList[position]
                val holder =
                    commentsRecyclerView.findViewHolderForAdapterPosition(position) as? CommentsAdapter.CommentViewHolder
                Log.d("CommentFragment", "Found holder for position $position: ${holder != null}")

                if (holder != null) {
                    Log.d("CommentFragment", "Using adapter toggle function for comment $commentId")
                    commentsAdapter.toggleRepliesVisibility(holder, comment)
                } else {
                    Log.d("CommentFragment", "Holder still not found after post, will try again")
                    // Try one more time after a short delay
                    commentsRecyclerView.postDelayed({
                        val retryHolder =
                            commentsRecyclerView.findViewHolderForAdapterPosition(position) as? CommentsAdapter.CommentViewHolder
                        retryHolder?.let {
                            commentsAdapter.toggleRepliesVisibility(it, comment)
                        }
                    }, 100)
                }
            }
        }
    }

    fun handleInitialComments(comments: List<CommentDto>) {
        Log.d("CommentFragment", "Starting to handle ${comments.size} comments")

        lifecycleScope.launch {
            try {
                commentsViewModel.clearSelections()
                val processedComments = mutableSetOf<Int>()
                val processedParents = mutableSetOf<Int>()

                // First handle parent comments
                comments.filter { it.parentCommentId == null }.forEach { comment ->
                    comment.commentId?.let {
                        commentsAdapter.selectComment(it)
                        processedComments.add(it)
                    }
                }

                // Then handle replies after adapter is updated
                commentsAdapter.onCommentsListChanged = { visibleComments ->
                    // Only process if we have new visible comments
                    val newVisibleComments = visibleComments.filter {
                        it.commentId != null && !processedComments.contains(it.commentId)
                    }

                    if (newVisibleComments.isNotEmpty()) {
                        lifecycleScope.launch {
                            comments.filter { it.parentCommentId != null }.forEach { comment ->
                                try {
                                    val parentId = comment.parentCommentId!!
                                    if (!processedParents.contains(parentId)) {
                                        when {
                                            // Direct reply - parent exists in visible comments
                                            visibleComments.any { it.commentId == parentId } -> {
                                                Log.d(
                                                    "CommentFragment",
                                                    "Processing parent $parentId for reply ${comment.commentId}"
                                                )
                                                processedParents.add(parentId)
                                                toggleReplySection(parentId, true)

                                                delay(100)

                                                if (!processedComments.contains(comment.commentId)) {
                                                    comment.commentId?.let {
                                                        commentsAdapter.selectComment(it)
                                                        processedComments.add(it)
                                                    }
                                                }
                                            }

                                            // Nested reply case
                                            else -> {
                                                val rootParentResponse =
                                                    withContext(Dispatchers.IO) {
                                                        retrofit.create(Comments::class.java)
                                                            .getRootParentComment(parentId)
                                                    }

                                                rootParentResponse.body()?.let { rootComment ->
                                                    if (!processedParents.contains(rootComment.commentId)) {
                                                        processedParents.add(rootComment.commentId!!)

                                                        if (visibleComments.any { it.commentId == rootComment.commentId }) {
                                                            toggleReplySection(
                                                                rootComment.commentId,
                                                                true
                                                            )
                                                        } else {
                                                            commentsAdapter.addNewComment(
                                                                rootComment
                                                            )
                                                            toggleReplySection(
                                                                rootComment.commentId,
                                                                true
                                                            )
                                                        }

                                                        delay(100)

                                                        if (!processedComments.contains(comment.commentId)) {
                                                            comment.commentId?.let {
                                                                commentsAdapter.selectComment(it)
                                                                processedComments.add(it)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "CommentFragment",
                                        "Error processing comment ${comment.commentId}",
                                        e
                                    )
                                }
                            }
                        }
                    }
                }

                comments.firstOrNull()?.commentId?.let { firstCommentId ->
                    val firstCommentView =
                        commentsRecyclerView.findViewWithTag<View>(firstCommentId)
                    firstCommentView?.let { scrollToView(it) }
                }

                // Initial update to trigger the callback
                commentsAdapter.updateComments(comments)

            } catch (e: Exception) {
                Log.e("CommentFragment", "Error in handleInitialComments", e)
            }
        }
    }

    private fun scrollToView(view: View) {
        commentsRecyclerView.post {
            val scrollBounds = Rect()
            commentsRecyclerView.getHitRect(scrollBounds)

            // Calculate offset to position the view slightly from the top
            val offsetPixels = resources.getDimensionPixelSize(R.dimen.comment_scroll_offset)

            if (!view.getLocalVisibleRect(scrollBounds)) {
                val location = IntArray(2)
                view.getLocationInWindow(location)
                commentsRecyclerView.smoothScrollBy(0, location[1] - offsetPixels)
            }
        }
    }

    override fun onDestroyView() {
        webSocketManager.disconnect()
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
                    val newComment = currentUser.userId.let {
                        CommentDto(
                            commentId = null,
                            userId = it,
                            username = currentUser.username,
                            postId = postId,
                            content = commentText,
                            sentiment = "Neutral",
                            timestamp = System.currentTimeMillis().toString(),
                            parentCommentId = if (isReplyMode) parentCommentId else null,
                            commentType = commentType
                        )
                    }

                    if (newComment != null) {
                        val response = insertComment(newComment)

                        if (response.isSuccessful && response.body()?.success == true) {
                            // Use the commentId directly from the response
                            val commentWithId = response.body()?.commentId?.let { id ->
                                newComment.copy(commentId = id)
                            } ?: newComment

                            withContext(Dispatchers.Main) {
                                if (isReplyMode && parentCommentId != null) {
                                    // Handle reply
                                    val parentComment =
                                        commentsAdapter.getPositionForComment(parentCommentId!!)
                                    if (parentComment != -1) {
                                        // Add to cache
                                        commentsViewModel.addCommentToCache(
                                            parentCommentId!!,
                                            commentWithId
                                        )

                                        if (!commentsViewModel.visibleReplySections.value.contains(
                                                parentCommentId
                                            )
                                        ) {
                                            commentsViewModel.toggleReplySection(
                                                parentCommentId!!,
                                                true
                                            )
                                        }

                                        // Update UI
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

                // Check if there are any comments
                if (commentsList.isEmpty()) {
                    Log.d("CommentFragment", "No comments found, generating demo comments")
                    generateDemoComments(postId)
                    fetchComments()
                } else {
                    Log.d("CommentFragment", "Successfully fetched ${commentsList.size} comments")
                    // Update the adapter with the fetched comments
                    commentsAdapter.updateComments(commentsList)
                }
            } catch (e: Exception) {
                // Log any errors that occur
                Log.e("CommentFragment", "Error fetching comments: ${e.message}")
            }
        }
    }

    private fun generateDemoComments(postId: Int) {
        // User data provided (userId -> username)
        val demoUsers = mapOf(
            5 to "aab",
            6 to "21j",
            14 to "ben10",
            8 to "abcdefg",
            7 to "bozo",
            11 to "Freedom",
            9 to "CJ",
            4 to "jake",
            15 to "alfath"
        )

        lifecycleScope.launch {
            try {
                val mainComments = listOf(
                    "This was one of the most visually stunning films I've seen all year!",
                    "The character development in this was really well done.",
                    "I didn't expect that plot twist - completely changed my perspective!",
                    "The soundtrack perfectly complemented the emotional scenes.",
                    "This deserves all the award nominations it's getting.",
                    "I've watched this three times already and notice new details each time.",
                    "The cinematography was absolutely breathtaking.",
                    "The lead actor's performance was incredible in this."
                )

                val replyComments = listOf(
                    "I agree! What was your favorite scene?",
                    "I thought the same thing. The director really outdid themselves.",
                    "Completely agree with your take on this.",
                    "Have you seen their other work? Just as impressive.",
                    "You convinced me to give it another watch!",
                    "I had the exact same reaction when I watched it!",
                    "Interesting perspective - I hadn't thought of it that way.",
                    "100% - couldn't have said it better myself."
                )

                // Add 3-5 main comments
                val commentCount = (3..5).random()
                val addedCommentIds = mutableListOf<Int>()
                val usedUserIds = mutableSetOf<Int>()

                // Get a random selection of users for main comments
                val mainCommentUsers = demoUsers.keys.shuffled().take(commentCount)

                for (i in 0 until commentCount) {
                    val userId = mainCommentUsers[i]
                    val username = demoUsers[userId] ?: continue
                    val commentText = mainComments.random()
                    val timestamp = System.currentTimeMillis() - (1000000L..10000000L).random()

                    usedUserIds.add(userId)

                    val newComment = CommentDto(
                        commentId = null,
                        userId = userId,
                        username = username,
                        postId = postId,
                        content = commentText,
                        sentiment = "Neutral",
                        timestamp = timestamp.toString(),
                        parentCommentId = null,
                        commentType = commentType
                    )

                    val response = insertComment(newComment)
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.commentId?.let {
                            addedCommentIds.add(it)
                        }
                    }

                    // Add small delay between adding comments
                    delay(200)
                }

                // Add 2-4 replies to random main comments
                val replyCount = (2..4).random()

                // Get remaining users for replies
                val availableReplyUsers = demoUsers.keys.filter { it !in usedUserIds }.shuffled()
                val replyUsers = if (availableReplyUsers.size >= replyCount) {
                    availableReplyUsers.take(replyCount)
                } else {
                    // If we need more users than available, reuse some
                    availableReplyUsers + demoUsers.keys.shuffled()
                        .take(replyCount - availableReplyUsers.size)
                }

                for (i in 0 until replyCount) {
                    if (addedCommentIds.isEmpty()) break

                    val parentId = addedCommentIds.random()
                    val userId = replyUsers[i]
                    val username = demoUsers[userId] ?: continue
                    val replyText = replyComments.random()
                    val timestamp = System.currentTimeMillis() - (100000L..5000000L).random()

                    val replyComment = CommentDto(
                        commentId = null,
                        userId = userId,
                        username = username,
                        postId = postId,
                        content = replyText,
                        sentiment = "Neutral",
                        timestamp = timestamp.toString(),
                        parentCommentId = parentId,
                        commentType = commentType
                    )

                    insertComment(replyComment)
                    delay(200)
                }

                // Reload comments to display the newly added ones
                val updatedComments = getCommentsForPost(postId)
                withContext(Dispatchers.Main) {
                    commentsAdapter.updateComments(updatedComments)
                }

            } catch (e: Exception) {
                Log.e("CommentFragment", "Error generating demo comments", e)
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

    private suspend fun insertComment(newComment: CommentDto): Response<CommentResponse> {
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
        webSocketManager.connect()  // Add this
    }

    /* private fun hideBottomNavBar() {
         activity?.findViewById<View>(R.id.bottom_nav_bar)?.visibility = View.GONE
     }

     private fun showBottomNavBar() {
         activity?.findViewById<View>(R.id.bottom_nav_bar)?.visibility = View.VISIBLE
     }

     */


}