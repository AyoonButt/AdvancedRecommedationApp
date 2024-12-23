package com.example.firedatabase_assis.explore

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.TrailerInteractions
import com.example.firedatabase_assis.postgres.UserEntity
import com.example.firedatabase_assis.postgres.UserTrailerInteraction
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VideoAdapter(
    private val videos: MutableList<Pair<String, Int>>, // List of pairs (videoKey, postId)
    private val lifecycleOwner: LifecycleOwner,
    private val userViewModel: UserViewModel // Pass UserViewModel for accessing user data
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    private var currentlyPlayingPlayer: YouTubePlayer? = null

    // Create the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)  // Replace with your API base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Instantiate your Posts API
    private val postsApi = retrofit.create(Posts::class.java)
    private val interactionsApi = retrofit.create(TrailerInteractions::class.java)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val youtubePlayerView: YouTubePlayerView =
            itemView.findViewById(R.id.youtube_player_view)
        var youTubePlayer: YouTubePlayer? = null
        lateinit var customPlayer: CustomPlayer

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
                        val (videoKey, _) = videos[bindingAdapterPosition]
                        youTubePlayer.cueVideo(videoKey, 0f)
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
            customPlayer = CustomPlayer(
                itemView.context,
                itemView,
                youTubePlayer,
                youtubePlayerView,
                videos[bindingAdapterPosition].second // Pass postId to CustomPlayer
            )
        }

        private fun setupAutoplayAndLoop(youTubePlayer: YouTubePlayer) {
            youTubePlayer.play()
            youTubePlayer.addListener(object : AbstractYouTubePlayerListener() {
                override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState
                ) {
                    when (state) {
                        PlayerConstants.PlayerState.ENDED -> {
                            youTubePlayer.seekTo(0f)
                            youTubePlayer.play()
                            customPlayer.incrementReplayCount()
                            customPlayer.startTrackingTime()
                        }

                        PlayerConstants.PlayerState.PLAYING -> {
                            currentlyPlayingPlayer?.pause()
                            currentlyPlayingPlayer = youTubePlayer
                            customPlayer.startTrackingTime()
                        }

                        else -> Unit
                    }
                }
            })
        }

        suspend fun saveInteractionData(bindingAdapterPosition: Int) {
            userViewModel.currentUser.observe(lifecycleOwner) { user ->
                user?.let { currentUser ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val interactionData =
                            createUserTrailerInteractionData(bindingAdapterPosition, currentUser)
                        interactionData.let {
                            try {
                                val response = interactionsApi.saveInteractionData(it)
                                if (response.isSuccessful) {
                                    Log.d(
                                        "SaveInteraction",
                                        "Data saved successfully: ${response.body()}"
                                    )
                                } else {
                                    Log.e(
                                        "SaveInteraction",
                                        "Failed to save data: ${response.errorBody()?.string()}"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("SaveInteraction", "Error saving data", e)
                            }
                        }
                    }
                } ?: Log.e("SaveInteraction", "User not found. Cannot save interaction.")
            }
        }


        private suspend fun createUserTrailerInteractionData(
            position: Int,
            user: UserEntity
        ): UserTrailerInteraction {
            val (_, postId) = videos[position]
            val post = withContext(Dispatchers.IO) { postsApi.fetchPostEntityById(postId) }

            return UserTrailerInteraction(
                user = user,
                post = post,
                timeSpent = customPlayer.getPlayTime(),
                replayCount = customPlayer.getReplayCount(),
                isMuted = customPlayer.getIsMuted(),
                likeState = customPlayer.getLikeState(),
                saveState = customPlayer.getSaveState(),
                commentButtonPressed = customPlayer.wasCommentButtonPressed(),
                commentMade = customPlayer.wasCommentMade(),
                timestamp = getCurrentTimestamp()
            )
        }
    }

    private fun getCurrentTimestamp(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.custom_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (videoKey, _) = videos[position]
        holder.youTubePlayer?.cueVideo(videoKey, 0f)
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            holder.saveInteractionData(holder.bindingAdapterPosition)
        }
        super.onViewDetachedFromWindow(holder)
    }

    fun addItems(newVideos: List<Pair<String, Int>>) {
        val initialSize = videos.size
        videos.addAll(newVideos)
        notifyItemRangeInserted(initialSize, newVideos.size)
    }
}
