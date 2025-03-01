package com.example.firedatabase_assis.interactions

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivitySingleItemBinding
import com.example.firedatabase_assis.explore.VideoAdapter
import com.example.firedatabase_assis.home_page.CommentFragment
import com.example.firedatabase_assis.home_page.MyPostAdapter
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.search.SearchViewModel
import com.example.firedatabase_assis.settings.ActivityNavigationHelper

class SingleItemActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleItemBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var viewModel: SingleItemViewModel


    private var currentPost: PostDto? = null
    private var viewStartTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation(R.id.bottom_menu_settings)
        ActivityNavigationHelper.setLastOpenedSettingsActivity(this::class.java)


        setupToolbar("Comments")

        userViewModel = UserViewModel.getInstance(application)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        viewModel = ViewModelProvider(this)[SingleItemViewModel::class.java]


        // Retrieve the post id using the new key
        val postWithComments = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_POST_COMMENTS, PostWithComments::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_POST_COMMENTS)
        }

        val openComments = intent.getBooleanExtra(EXTRA_OPEN_COMMENTS, false)

        Log.d("SingleItemActivity", "Received postWithComments: $postWithComments")
        Log.d("SingleItemActivity", "Intent extras: ${intent.extras?.keySet()?.joinToString()}")


        viewStartTime = System.currentTimeMillis()

        // Use the post from PostWithComments
        postWithComments?.let { postData ->
            val post = postData.post
            currentPost = post

            // Get the comment type from the first comment, or default to "post"
            val commentType = postData.comments.firstOrNull()?.commentType ?: "post"

            if (commentType == "trailer") {
                setupVideoPlayer(post, openComments, postData.comments)
            } else {
                setupPost(post)
                if (openComments) {
                    post.postId?.let { postId ->
                        showComments(postId, postData.comments)
                    }
                }
            }
        } ?: run {
            Log.e("SingleItemActivity", "No post data received")
            finish()
        }
    }


    private fun setupVideoPlayer(
        post: PostDto,
        openComments: Boolean = false,
        comments: List<CommentDto> = emptyList()
    ) {
        binding.postLayout.visibility = View.GONE
        binding.playerContainer.visibility = View.VISIBLE

        val videoAdapter = VideoAdapter(
            videos = mutableListOf(post),  // Single video in this case
            lifecycleOwner = this,
            currentUser = userViewModel.currentUser.value,
            searchViewModel = searchViewModel,
            userViewModel = userViewModel
        )

        // Set up RecyclerView for video
        binding.playerContainer.apply {
            layoutManager = LinearLayoutManager(this@SingleItemActivity)
            adapter = videoAdapter
        }

        if (openComments) {
            post.postId?.let { postId ->
                showComments(postId, comments)
            }
        }
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setTitle(title)
        }
        binding.toolbar.setNavigationOnClickListener {
            // Navigate back to SettingsActivity
            val intent = Intent(this, InteractionsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            ActivityNavigationHelper.removeLastOpenedSettingsActivity()
            startActivity(intent)
            finish()
        }
    }

    private fun setupPost(post: PostDto) {
        binding.playerContainer.visibility = View.GONE
        binding.postLayout.visibility = View.VISIBLE

        val postAdapter = MyPostAdapter(
            context = this,
            movies = mutableListOf(post),
            userViewModel = userViewModel,
            searchViewModel = searchViewModel
        )

        binding.postLayout.apply {
            layoutManager = LinearLayoutManager(this@SingleItemActivity)
            adapter = postAdapter
        }
    }


    private fun showComments(postId: Int, comments: List<CommentDto>) {
        val fragmentContainer = binding.fragmentContainer
        fragmentContainer.visibility = View.VISIBLE

        val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = 0
        fragmentContainer.layoutParams = layoutParams

        // Use the comment type from the first comment, or default to "post"
        val commentType = comments.firstOrNull()?.commentType ?: "post"

        val commentFragment = CommentFragment(
            postId = postId,
            commentType = commentType
        )

        // Replace fragment and wait for it to be ready
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, commentFragment)
            .addToBackStack(null)
            .commit()

        // Wait for fragment to be ready before handling comments
        supportFragmentManager.executePendingTransactions()
        commentFragment.onReady {
            commentFragment.handleInitialComments(comments)
        }
    }

    companion object {
        private const val EXTRA_POST_COMMENTS = "extra_post_comments"
        private const val EXTRA_OPEN_COMMENTS = "extra_open_comments"

        fun newIntent(
            context: Context,
            postComments: PostWithComments,
            openComments: Boolean = false
        ): Intent {
            return Intent(context, SingleItemActivity::class.java).apply {
                putExtra(EXTRA_POST_COMMENTS, postComments)
                putExtra(EXTRA_OPEN_COMMENTS, openComments)
            }
        }
    }
}
