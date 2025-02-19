package com.example.firedatabase_assis.interactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivitySingleItemBinding
import com.example.firedatabase_assis.explore.CustomPlayer
import com.example.firedatabase_assis.home_page.CommentFragment
import com.example.firedatabase_assis.home_page.DoubleClickListener
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.search.SearchViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.squareup.picasso.Picasso

class SingleItemActivity : BaseActivity() {
    private lateinit var binding: ActivitySingleItemBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var viewModel: SingleItemViewModel


    private lateinit var posterImage: ImageView
    private lateinit var title: TextView
    private lateinit var overview: TextView
    private lateinit var heart: ImageView
    private lateinit var insideHeart: ImageView
    private lateinit var comments: ImageView
    private lateinit var saved: ImageView
    private lateinit var info: ImageView

    private var currentPost: PostDto? = null
    private var viewStartTime: Long = 0
    private var likeState: Boolean = false
    private var saveState: Boolean = false
    private var commentButtonPressed: Boolean = false
    private var commentMade: Boolean = false

    private var currentlyPlayingPlayer: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)

        userViewModel = UserViewModel.getInstance(application)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        viewModel = ViewModelProvider(this)[SingleItemViewModel::class.java]

        initializeViews()

        // Retrieve the post id using the new key
        val postId = intent.getIntExtra(EXTRA_POST_ID, -1)
        val openComments = intent.getBooleanExtra(EXTRA_OPEN_COMMENTS, false)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        viewStartTime = System.currentTimeMillis()

        viewModel.state.observe(this) { state ->
            when {
                state.isLoading -> {
                    // Show loading if needed
                }

                state.error != null -> {
                    // Show error if needed
                }

                state.item != null -> {
                    currentPost = state.item
                    setupPost(state.item)
                    if (openComments) {
                        showComments(postId)
                    }
                }
            }
        }
    }

    private fun initializeViews() {
        posterImage = findViewById(R.id.poster_image)
        title = findViewById(R.id.title)
        overview = findViewById(R.id.movie_caption)
        heart = findViewById(R.id.heart)
        insideHeart = findViewById(R.id.insideHeart)
        comments = findViewById(R.id.comments)
        saved = findViewById(R.id.saved)
        info = findViewById(R.id.info)

        // Initialize states
        heart.tag = "unliked"
        saved.tag = "not_saved"
    }

    private fun setupPost(post: PostDto) {
        title.text = post.title
        overview.text = post.overview

        // Load image using Picasso
        val baseURL = "https://image.tmdb.org/t/p/original${post.posterPath}"
        Picasso.get()
            .load(baseURL)
            .resize(350, 500)
            .centerCrop()
            .into(posterImage)

        // Check for video type, and setup accordingly
        if (post.type.equals("video", ignoreCase = true)) {
            setupVideoPlayer(post)
        } else {
            setupClickListeners(post)
        }
    }


    private fun setupVideoPlayer(post: PostDto) {
        // Initialize the YouTubePlayerView from the layout
        val youTubePlayerView: YouTubePlayerView = findViewById(R.id.youtube_player_view)

        // Ensure the YouTubePlayerView is visible when it's a video
        val playerUi: FrameLayout = findViewById(R.id.player_ui)
        playerUi.visibility = View.VISIBLE

        // Initialize your custom player with the player instance and other UI elements
        currentlyPlayingPlayer?.let {
            CustomPlayer(
                context = this,
                customPlayerUi = playerUi,
                youTubePlayer = it,  // Corrected typo here
                youTubePlayerView = youTubePlayerView,
                video = post,  // Assuming 'toVideoDto()' is an extension to convert post data to video
                searchViewModel = searchViewModel,
                userViewModel = userViewModel
            )
        }
    }

    private fun setupClickListeners(post: PostDto) {
        // Double click listener for the post image
        posterImage.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View?) {
                animateHeartLike()
                updateLikeState(true, post)
                post.postId?.let { updateInteraction(it) }
            }
        })

        heart.setOnClickListener {
            updateLikeState(!likeState, post)
            post.postId?.let { it1 -> updateInteraction(it1) }
        }

        comments.setOnClickListener {
            commentButtonPressed = true
            post.postId?.let { it1 -> showComments(it1) }
            post.postId?.let { it1 -> updateInteraction(it1) }
        }

        saved.setOnClickListener {
            saveState = !saveState
            saved.setImageResource(
                if (saveState) R.drawable.icon_bookmark_filled
                else R.drawable.icon_bookmark_unfilled
            )
            saved.tag = if (saveState) "saved" else "not_saved"
            post.postId?.let { it1 -> updateInteraction(it1) }
        }

        info.setOnClickListener {
            val isMovie = post.type.lowercase() == "movie"
            searchViewModel.navigate(
                SearchViewModel.NavigationState.ShowPoster(
                    post.tmdbId, isMovie
                )
            )
        }
    }

    private fun updateLikeState(newState: Boolean, post: PostDto) {
        likeState = newState
        heart.setImageResource(
            if (likeState) R.drawable.heart_red
            else R.drawable.heart_outline
        )
        heart.tag = if (likeState) "liked" else "unliked"
        if (likeState) {
            post.postId?.let { viewModel.updateLikeCount(it) }
        }
    }

    private fun animateHeartLike() {
        heart.setImageResource(R.drawable.heart_red)
        heart.tag = "liked"

        val zoomInAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val zoomOutAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_out)

        val animationSet = AnimationSet(true).apply {
            addAnimation(zoomInAnim)
            addAnimation(zoomOutAnim)
        }

        insideHeart.setImageResource(R.drawable.heart_white)
        insideHeart.visibility = View.VISIBLE

        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                insideHeart.visibility = View.INVISIBLE
            }
        })

        insideHeart.startAnimation(animationSet)
    }

    private fun updateInteraction(postId: Int) {
        val userId = userViewModel.currentUser.value?.userId ?: return
        val currentTime = System.currentTimeMillis()

        viewModel.updateInteraction(
            userId = userId,
            postId = postId,
            likeState = likeState,
            saveState = saveState,
            commentButtonPressed = commentButtonPressed,
            commentMade = commentMade,
            startTime = viewStartTime,
            endTime = currentTime
        )
    }

    private fun showComments(postId: Int) {
        val fragmentContainer = binding.fragmentContainer
        fragmentContainer.visibility = View.VISIBLE

        val transaction = supportFragmentManager.beginTransaction()
        val commentFragment = CommentFragment(postId)
        transaction.replace(R.id.fragment_container, commentFragment)
        transaction.addToBackStack(null)
        transaction.commit()

        commentFragment.view?.addOnAttachStateChangeListener(object :
            View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                fragmentContainer.setOnTouchListener(null)
                commentMade = true
                updateInteraction(postId)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPost?.postId?.let { updateInteraction(it) }
    }

    companion object {
        private const val EXTRA_POST_ID = "extra_post_id"
        private const val EXTRA_OPEN_COMMENTS = "extra_open_comments"

        fun newIntent(context: Context, postDto: PostDto, openComments: Boolean = false): Intent {
            return Intent(context, SingleItemActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postDto.postId)
                putExtra(EXTRA_OPEN_COMMENTS, openComments)
            }
        }
    }

}
