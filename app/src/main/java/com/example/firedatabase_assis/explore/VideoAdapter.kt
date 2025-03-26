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
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.TrailerInteractionDto
import com.example.firedatabase_assis.postgres.TrailerInteractions
import com.example.firedatabase_assis.postgres.UserEntity
import com.example.firedatabase_assis.search.SearchViewModel
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
    private val videos: MutableList<PostDto>,
    private val lifecycleOwner: LifecycleOwner,
    private val currentUser: UserEntity?,
    private val searchViewModel: SearchViewModel,
    private val userViewModel: UserViewModel,
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    private var currentlyPlayingPlayer: YouTubePlayer? = null

    // Create the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val interactionsApi = retrofit.create(TrailerInteractions::class.java)

    private var startTimestamp = ""

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val youtubePlayerView: YouTubePlayerView =
            itemView.findViewById(R.id.youtube_player_view)
        var youTubePlayer: YouTubePlayer? = null
        lateinit var customPlayer: CustomPlayer

        // Track the currently bound video to ensure we're working with the right data
        private var boundVideoId: Int? = null

        init {
            lifecycleOwner.lifecycle.addObserver(youtubePlayerView)

            // Setup the YouTube player
            initializeYouTubePlayer()
        }

        private fun initializeYouTubePlayer() {
            youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    this@ViewHolder.youTubePlayer = youTubePlayer
                    startTimestamp = getCurrentTimestamp()

                    // Get current binding position
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && position < videos.size) {
                        // Get video at current position
                        val video = videos[position]
                        boundVideoId = video.postId

                        // Log which video is being initialized
                        Log.d(
                            "VideoAdapter", "Initializing player at position $position:" +
                                    " postId=${video.postId}, tmdbId=${video.tmdbId}, type=${video.type}"
                        )

                        // Load the video
                        video.videoKey.let {
                            youTubePlayer.cueVideo(it, 0f)
                        }

                        // Setup custom player
                        setupCustomPlayer(youTubePlayer, video)
                    } else {
                        Log.e(
                            "VideoAdapter",
                            "Invalid position: $position, videos size: ${videos.size}"
                        )
                    }
                }

                override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError
                ) {
                    Log.e("YouTubePlayer", "Error initializing player: $error")
                }
            }, IFramePlayerOptions.Builder().controls(0).build())
        }

        fun bind(position: Int) {
            if (position >= videos.size) {
                Log.e("VideoAdapter", "Attempted to bind invalid position: $position")
                return
            }

            val video = videos[position]
            boundVideoId = video.postId

            Log.d(
                "VideoAdapter", "Binding position $position with video:" +
                        " postId=${video.postId}, tmdbId=${video.tmdbId}, type=${video.type}"
            )

            // If player is ready, set the video
            youTubePlayer?.let { player ->
                video.videoKey.let {
                    player.cueVideo(it, 0f)
                }

                // If CustomPlayer is already initialized, update its video reference
                if (::customPlayer.isInitialized) {
                    // We need to update the CustomPlayer's reference to the video
                    // but we can't directly update it, so we'll recreate it
                    setupCustomPlayer(player, video)
                }
            }

            // Set up info button click listener directly in the ViewHolder
            // This ensures the correct video data is used when clicked
            itemView.findViewById<View>(R.id.info).setOnClickListener {
                try {
                    // Use the currently bound video (which should be the most up-to-date)
                    val currentVideo = videos[bindingAdapterPosition]

                    // Determine if it's a movie based on the video type
                    val isMovie = currentVideo.type == "movie"

                    Log.d(
                        "VideoAdapter", "Info button clicked for:" +
                                " postId=${currentVideo.postId}, tmdbId=${currentVideo.tmdbId}," +
                                " type=${currentVideo.type}, isMovie=$isMovie"
                    )

                    // Navigate using the SearchViewModel
                    searchViewModel.navigate(
                        SearchViewModel.NavigationState.ShowPoster(
                            currentVideo.tmdbId,
                            isMovie
                        )
                    )
                } catch (e: Exception) {
                    Log.e("VideoAdapter", "Error in info button click", e)
                }
            }
        }

        private fun setupCustomPlayer(youTubePlayer: YouTubePlayer, video: PostDto) {
            customPlayer = CustomPlayer(
                itemView.context,
                itemView,
                youTubePlayer,
                video,
                searchViewModel,
                userViewModel,
                // Pass a lambda that will be called when info button is clicked
                infoButtonClickListener = {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && position < videos.size) {
                        val currentVideo = videos[position]
                        val isMovie = currentVideo.type == "movie"

                        Log.d(
                            "VideoAdapter", "Info button clicked via lambda for:" +
                                    " postId=${currentVideo.postId}, tmdbId=${currentVideo.tmdbId}," +
                                    " type=${currentVideo.type}, isMovie=$isMovie"
                        )

                        searchViewModel.navigate(
                            SearchViewModel.NavigationState.ShowPoster(
                                currentVideo.tmdbId,
                                isMovie
                            )
                        )
                    } else {
                        Log.e("VideoAdapter", "Invalid position for info click: $position")
                    }
                }
            )
            setupAutoplayAndLoop(youTubePlayer)
        }

        private fun setupAutoplayAndLoop(youTubePlayer: YouTubePlayer) {
            youTubePlayer.play()
            customPlayer.setAutoStart(true)
            currentlyPlayingPlayer = youTubePlayer
        }

        suspend fun saveTrailerInteractionData(bindingAdapterPosition: Int) =
            withContext(Dispatchers.IO) {
                try {
                    Log.d("SaveInteraction", "User found: $currentUser")

                    val interactionData =
                        currentUser?.let {
                            createUserTrailerInteractionDto(bindingAdapterPosition, it)
                        }

                    if (interactionData == null) {
                        Log.e("SaveInteraction", "Failed to create interaction data")
                        return@withContext
                    }

                    val response = interactionsApi.saveInteractionData(interactionData)

                    if (response.isSuccessful) {
                        Log.d("SaveInteraction", "Data saved successfully: ${response.body()}")
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

        private fun createUserTrailerInteractionDto(
            position: Int,
            user: UserEntity
        ): TrailerInteractionDto? {
            if (position < 0 || position >= videos.size) {
                Log.e("VideoAdapter", "Invalid position for interaction: $position")
                return null
            }

            val video = videos[position]
            val postId = video.postId
            val currentTime = getCurrentTimestamp()

            return postId?.let {
                TrailerInteractionDto(
                    interactionId = null,
                    userId = user.userId,
                    postId = it,
                    startTimestamp = startTimestamp,
                    endTimestamp = currentTime,
                    replayCount = if (::customPlayer.isInitialized) customPlayer.getReplayCount() else 0,
                    isMuted = if (::customPlayer.isInitialized) customPlayer.getIsMuted() else false,
                    likeState = if (::customPlayer.isInitialized) customPlayer.getLikeState() else false,
                    saveState = if (::customPlayer.isInitialized) customPlayer.getSaveState() else false,
                    commentButtonPressed = if (::customPlayer.isInitialized) customPlayer.wasCommentButtonPressed() else false
                )
            }
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
        // Bind the video data to the view holder
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                holder.saveTrailerInteractionData(position)
            }
        }
        super.onViewDetachedFromWindow(holder)
    }

    fun clearAndAddItems(newItems: List<PostDto>) {
        // Clear existing items
        videos.clear()

        // Add new items
        videos.addAll(newItems)

    }

    fun addItems(newItems: List<PostDto>) {
        if (newItems.isEmpty()) return

        // Track the insertion position
        val startPosition = videos.size

        // Add to the end of the list
        videos.addAll(newItems)

        // Notify adapter about the range insertion
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    fun getItems(): List<PostDto> {
        return videos
    }
}