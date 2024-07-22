package com.example.firedatabase_assis.explore

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideoAdapter(
    private val videoKeys: MutableList<String>,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    private var currentlyPlayingPlayer: YouTubePlayer? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val youtubePlayerView: YouTubePlayerView =
            itemView.findViewById(R.id.youtube_player_view)
        var youTubePlayer: YouTubePlayer? = null
        private lateinit var customPlayer: CustomPlayer

        init {
            lifecycleOwner.lifecycle.addObserver(youtubePlayerView)
            youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    Log.d(
                        "YouTubePlayer",
                        "Player initialized for position: $bindingAdapterPosition"
                    )
                    this@ViewHolder.youTubePlayer = youTubePlayer
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        youTubePlayer.cueVideo(videoKeys[bindingAdapterPosition], 0f)
                    }
                    setupCustomPlayer(youTubePlayer)
                    setupAutoplayAndLoop(youTubePlayer)
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    Log.e("YouTubePlayer", "Error initializing player: $error")
                }
            }, IFramePlayerOptions.Builder().controls(0).build())
        }

        private fun setupCustomPlayer(youTubePlayer: YouTubePlayer) {
            customPlayer =
                CustomPlayer(itemView.context, itemView, youTubePlayer, youtubePlayerView)
        }

        private fun setupAutoplayAndLoop(youTubePlayer: YouTubePlayer) {
            youTubePlayer.play()
            youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        youTubePlayer.seekTo(0f)
                        youTubePlayer.play()
                    }
                }
            })
        }

        fun bind(videoKey: String) {
            youTubePlayer?.cueVideo(videoKey, 0f)
            youTubePlayer?.play()
            setCurrentlyPlayingPlayer(youTubePlayer)
        }

        private fun setCurrentlyPlayingPlayer(youTubePlayer: YouTubePlayer?) {
            currentlyPlayingPlayer?.pause()
            currentlyPlayingPlayer = youTubePlayer
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.custom_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(videoKeys[position])
    }

    override fun getItemCount(): Int = videoKeys.size

    fun addVideos(newVideoKeys: List<String>) {
        val start = videoKeys.size
        videoKeys.addAll(newVideoKeys)
        notifyItemRangeInserted(start, newVideoKeys.size)
    }
}
