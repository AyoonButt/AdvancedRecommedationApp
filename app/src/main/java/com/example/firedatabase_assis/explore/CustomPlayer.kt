package com.example.firedatabase_assis.explore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.Posts
import com.example.firedatabase_assis.database.UserTrailerInteractions
import com.example.firedatabase_assis.home_page.CommentFragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class CustomPlayer(
    private val context: Context,
    customPlayerUi: View,
    private val youTubePlayer: YouTubePlayer,
    youTubePlayerView: YouTubePlayerView,
    private val PostID: Int // Add postId as a parameter
) : AbstractYouTubePlayerListener() {

    private val playerTracker: YouTubePlayerTracker = YouTubePlayerTracker()
    private val panel: View = customPlayerUi.findViewById(R.id.panel)
    private val insideHeart = customPlayerUi.findViewById<ImageView>(R.id.insideHeart)
    private val muteIcon = customPlayerUi.findViewById<ImageView>(R.id.unmute)

    private var isMuted = false
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
    private var likeState: Boolean = false
    private var saveState: Boolean = false
    private var commentButtonPressed: Boolean = false
    private var commentMade: Boolean = false
    private var replayCount = 0

    init {
        youTubePlayer.removeListener(playerTracker)
        youTubePlayer.addListener(this)
        initViews(customPlayerUi)

        // Disable built-in touch interactions
        youTubePlayerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> true
                else -> false
            }
        }
    }

    private fun initViews(customPlayerUi: View) {
        val heart = customPlayerUi.findViewById<ImageView>(R.id.heart)
        val comments = customPlayerUi.findViewById<ImageView>(R.id.comments)
        val saved = customPlayerUi.findViewById<ImageView>(R.id.saved)

        heart.setOnClickListener {
            toggleLike(heart)
        }

        comments.setOnClickListener {
            commentButtonPressed = true
            val activity = context as AppCompatActivity
            val fragmentContainer = activity.findViewById<View>(R.id.fragment_container)
            if (fragmentContainer != null) {
                fragmentContainer.visibility = View.VISIBLE
                val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = 0
                fragmentContainer.layoutParams = layoutParams

                activity.supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        CommentFragment(PostID)
                    )
                    .addToBackStack(null)
                    .commit()

                // Update timestamp
                updateInteractionTimestamp(PostID)
            }
        }


        saved.setOnClickListener {
            saveState = saved.tag != "saved"
            saved.setImageResource(
                if (saveState) R.drawable.icon_bookmark_filled else R.drawable.icon_bookmark_videos
            )
            saved.tag = if (saveState) "saved" else "unsaved"

            // Update timestamp
            updateInteractionTimestamp(PostID)
        }


        muteIcon.setOnClickListener {
            toggleMute()
        }
    }

    private fun toggleLike(heart: ImageView) {
        likeState = heart.tag == "liked"
        heart.setImageResource(
            if (likeState) {
                heart.tag = "unliked"
                R.drawable.heart_white_outline
            } else {
                heart.tag = "liked"
                R.drawable.heart_red
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
        updateLikeCount(PostID)
        updateInteractionTimestamp(PostID)
    }


    private fun toggleMute() {
        isMuted = !isMuted
        youTubePlayer.setVolume(if (isMuted) 0 else 100)
        muteIcon.setImageResource(if (isMuted) R.drawable.unmute_white else R.drawable.mute_white)
    }

    override fun onReady(youTubePlayer: YouTubePlayer) {
        youTubePlayer.play()
    }

    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerState) {
        panel.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        if (state == PlayerState.ENDED) {
            youTubePlayer.seekTo(0f) // Loop the video
            youTubePlayer.play()
        }
    }

    private fun updateLikeCount(postId: Int) {
        transaction {
            val currentLikeCount = Posts.slice(Posts.trailerLikeCount)
                .select { Posts.postId eq postId }
                .singleOrNull()?.get(Posts.trailerLikeCount) ?: 0

            Posts.update({ Posts.postId eq postId }) {
                it[trailerLikeCount] = currentLikeCount + 1
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

    // Stop tracking play time
    fun stopTrackingTime() {
        isTrackingTime = false
        handler.removeCallbacks(trackingRunnable)
        // Now you can use the playTime value for further processing
        // For example, saving it to the database
    }

    // Reset the play time
    fun resetPlayTime() {
        playTime = 0
    }

    // Get the play time
    fun getPlayTime(): Long {
        return playTime
    }

    // Get current states
    fun getLikeState(): Boolean = likeState
    fun getSaveState(): Boolean = saveState
    fun wasCommentButtonPressed(): Boolean = commentButtonPressed
    fun wasCommentMade(): Boolean = commentMade

    // Increment replay count
    fun incrementReplayCount() {
        replayCount++
    }

    fun getReplayCount(): Int {
        return replayCount
    }

    // Get the mute state
    fun getIsMuted(): Boolean {
        return isMuted
    }

    // Set comment made status
    fun setCommentMade(status: Boolean) {
        commentMade = status
    }

    private fun getCurrentTimestamp(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    private fun updateInteractionTimestamp(postId: Int) {
        transaction {
            UserTrailerInteractions.update({ UserTrailerInteractions.postId eq postId }) {
                it[timestamp] = getCurrentTimestamp()
            }
        }
    }

}
