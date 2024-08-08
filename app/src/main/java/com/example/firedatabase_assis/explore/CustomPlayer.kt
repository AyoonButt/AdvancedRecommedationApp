package com.example.firedatabase_assis.explore

import android.content.Context
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
import com.example.firedatabase_assis.database.MovieDatabase
import com.example.firedatabase_assis.home_page.CommentFragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class CustomPlayer(
    private val context: Context,
    customPlayerUi: View,
    private val youTubePlayer: YouTubePlayer,
    private val youTubePlayerView: YouTubePlayerView
) : AbstractYouTubePlayerListener() {

    private val playerTracker: YouTubePlayerTracker = YouTubePlayerTracker()
    private val panel: View = customPlayerUi.findViewById(R.id.panel)
    private val progressbar: View = customPlayerUi.findViewById(R.id.progressbar)
    private val insideHeart = customPlayerUi.findViewById<ImageView>(R.id.insideHeart)
    private val muteIcon = customPlayerUi.findViewById<ImageView>(R.id.unmute)

    private var isMuted = false

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
                        CommentFragment(
                            postId = 0,
                            MovieDatabase.getDatabase(context.applicationContext).commentDao()
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        saved.setOnClickListener {
            saved.setImageResource(if (saved.tag == "saved") R.drawable.icon_bookmark_videos else R.drawable.icon_bookmark_filled)
            saved.tag = if (saved.tag == "saved") "unsaved" else "saved"
        }

        muteIcon.setOnClickListener {
            toggleMute()
        }
    }

    private fun toggleLike(heart: ImageView) {
        heart.setImageResource(
            if (heart.tag == "liked") {
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
}
