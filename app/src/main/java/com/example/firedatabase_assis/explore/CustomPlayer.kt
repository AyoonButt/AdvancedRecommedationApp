package com.example.firedatabase_assis.explore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.home_page.CommentFragment
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.InteractionStates
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.TrailerInteractions
import com.example.firedatabase_assis.search.SearchViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CustomPlayer(
    private val context: Context,
    customPlayerUi: View,
    private val youTubePlayer: YouTubePlayer,
    private val video: PostDto,
    private val searchViewModel: SearchViewModel,
    private val userViewModel: UserViewModel,
    // Add a lambda function parameter for info button click
    private val infoButtonClickListener: (() -> Unit)? = null
) : AbstractYouTubePlayerListener() {

    private val playerTracker: YouTubePlayerTracker = YouTubePlayerTracker()
    private val panel: View = customPlayerUi.findViewById(R.id.panel)
    private val insideHeart = customPlayerUi.findViewById<ImageView>(R.id.insideHeart)
    private val muteIcon = customPlayerUi.findViewById<ImageView>(R.id.unmute)

    private var isMuted = false
    private var isPlaying = true // Track play/pause state
    private var playTime: Long = 0
    private var isTrackingTime = false
    private val handler = Handler(Looper.getMainLooper())
    private val trackingRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isTrackingTime) {
                playTime += 1000 // Increment play time by 1 second
                handler.postDelayed(this, 1000) // Schedule the next increment
            }
        }
    }

    // Add new fields to store the states
    private var isMovie: Boolean = false
    private var likeState: Boolean = false
    private var saveState: Boolean = false
    private var commentButtonPressed: Boolean = false
    private var replayCount = 0
    private var autoStart = true // Default to auto-start

    // Create the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instantiate your Posts API
    private val postsApi = retrofit.create(Posts::class.java)
    private val interactionsApi = retrofit.create(TrailerInteractions::class.java)

    init {
        youTubePlayer.removeListener(playerTracker)
        youTubePlayer.addListener(this)

        isMovie = video.type == "movie"

        // Log initialization details
        Log.d(
            "CustomPlayer", "Initializing player for video:" +
                    " postId=${video.postId}, tmdbId=${video.tmdbId}," +
                    " type=${video.type}, isMovie=$isMovie"
        )

        initViews(customPlayerUi)

        // Start tracking playback time
        startTrackingTime()
    }

    private fun initViews(customPlayerUi: View) {
        val heart = customPlayerUi.findViewById<ImageView>(R.id.heart)
        val comments = customPlayerUi.findViewById<ImageView>(R.id.comments)
        val saved = customPlayerUi.findViewById<ImageView>(R.id.saved)
        val info = customPlayerUi.findViewById<ImageView>(R.id.info)

        CoroutineScope(Dispatchers.IO).launch {
            val userId = userViewModel.currentUser.value?.userId ?: return@launch
            val states = video.postId?.let { getStates(userId, it) } ?: InteractionStates()

            withContext(Dispatchers.Main) {
                // Update class-level states
                this@CustomPlayer.likeState = states.isLiked
                this@CustomPlayer.saveState = states.isSaved

                // Update UI
                heart.setImageResource(
                    if (likeState) R.drawable.heart_red else R.drawable.heart_white_outline
                )
                heart.tag = if (likeState) "liked" else "unliked"

                saved.setImageResource(
                    if (saveState) R.drawable.icon_bookmark_filled else R.drawable.icon_bookmark_videos
                )
                saved.tag = if (saveState) "saved" else "unsaved"
            }
        }

        heart.setOnClickListener {
            toggleLike(heart)
        }

        saved.setOnClickListener {
            toggleSave(saved)
        }

        comments.setOnClickListener {
            commentButtonPressed = true
            val activity = context as AppCompatActivity
            val fragmentContainer = activity.findViewById<View>(R.id.fragment_container)
            if (fragmentContainer != null) {
                fragmentContainer.visibility = View.VISIBLE

                video.postId?.let { postId ->
                    CommentFragment(postId, commentType = "trailer").also { fragment ->
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }

        muteIcon.setOnClickListener {
            toggleMute()
        }

        // Set up info button to use the lambda if provided, otherwise use direct navigation
        info.setOnClickListener {
            if (infoButtonClickListener != null) {
                // Use the lambda from the adapter to get fresh data
                infoButtonClickListener.invoke()
            } else {
                // Fallback to direct navigation if no lambda provided
                try {
                    Log.d(
                        "CustomPlayer", "Info button clicked (direct):" +
                                " postId=${video.postId}, tmdbId=${video.tmdbId}," +
                                " type=${video.type}, isMovie=$isMovie"
                    )

                    // For backwards compatibility, still support direct navigation
                    searchViewModel.navigate(
                        SearchViewModel.NavigationState.ShowPoster(
                            video.tmdbId,
                            isMovie
                        )
                    )
                } catch (e: Exception) {
                    Log.e("CustomPlayer", "Error in direct info button click", e)
                }
            }
        }
    }

    private fun toggleLike(heart: ImageView) {
        likeState = heart.tag != "liked" // Fix the logic here

        heart.setImageResource(
            if (likeState) {
                heart.tag = "liked"     // Update tag to match the new state
                R.drawable.heart_red
            } else {
                heart.tag = "unliked"   // Update tag to match the new state
                R.drawable.heart_white_outline
            }
        )

        // Perform animation
        val zoomInAnim = AnimationUtils.loadAnimation(context, R.anim.zoom_in)
        val zoomOutAnim = AnimationUtils.loadAnimation(context, R.anim.zoom_out)

        val animationSet = AnimationSet(true).apply {
            addAnimation(zoomInAnim)
            addAnimation(zoomOutAnim)
        }

        insideHeart.setImageResource(R.drawable.heart_white)
        insideHeart.visibility = View.VISIBLE

        // Set AnimationListener to handle animation end
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                insideHeart.visibility = View.INVISIBLE
            }
        })

        // Start the animation
        insideHeart.startAnimation(animationSet)

        // Update like count and timestamp
        video.postId?.let {
            updateLikeCount(it)
        }
    }

    private fun toggleSave(saved: ImageView) {
        saveState = !saveState
        saved.setImageResource(
            if (saveState) R.drawable.icon_bookmark_filled
            else R.drawable.icon_bookmark_videos
        )
        saved.tag = if (saveState) "saved" else "unsaved"
    }

    private fun toggleMute() {
        isMuted = !isMuted
        youTubePlayer.setVolume(if (isMuted) 0 else 100)
        muteIcon.setImageResource(if (isMuted) R.drawable.mute_white else R.drawable.unmute_white)
    }

    override fun onReady(youTubePlayer: YouTubePlayer) {
        if (autoStart) {
            youTubePlayer.play()
            isPlaying = true
        }
    }

    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerState) {
        panel.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))

        when (state) {
            PlayerState.ENDED -> {
                // Loop the video when it ends
                youTubePlayer.seekTo(0f)
                youTubePlayer.play()
                replayCount++
                isPlaying = true
            }

            PlayerState.PLAYING -> {
                isPlaying = true
                // Start tracking play time if not already started
                if (!isTrackingTime) {
                    startTrackingTime()
                }
            }

            PlayerState.PAUSED -> {
                isPlaying = false
            }

            else -> {} // Handle other states if needed
        }
    }

    private fun updateLikeCount(postId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postsApi.updateTrailerLikeCount(postId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Start tracking play time
    fun startTrackingTime() {
        if (!isTrackingTime) {
            isTrackingTime = true
            handler.post(trackingRunnable)
        }
    }

    // Set auto-start behavior
    fun setAutoStart(autoStartEnabled: Boolean) {
        autoStart = autoStartEnabled
    }

    // Play/pause control methods
    fun play() {
        youTubePlayer.play()
        isPlaying = true
    }

    fun pause() {
        youTubePlayer.pause()
        isPlaying = false
    }

    fun isPlaying(): Boolean {
        return isPlaying
    }

    // Get current states
    fun getLikeState(): Boolean = likeState
    fun getSaveState(): Boolean = saveState
    fun wasCommentButtonPressed(): Boolean = commentButtonPressed

    fun getReplayCount(): Int {
        return replayCount
    }

    // Get the mute state
    fun getIsMuted(): Boolean {
        return isMuted
    }

    private suspend fun getStates(userId: Int, postId: Int): InteractionStates {
        return try {
            val response = interactionsApi.getTrailerInteractionStates(userId, postId)
            if (response.isSuccessful) {
                response.body() ?: InteractionStates()
            } else {
                InteractionStates()  // Default to false states if request fails
            }
        } catch (e: Exception) {
            Log.e("CustomPlayer", "Error getting interaction states: ${e.message}")
            InteractionStates()  // Default to false states on error
        }
    }
}