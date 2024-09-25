import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.UserTrailerInteractions
import com.example.firedatabase_assis.explore.CustomPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class VideoAdapter(
    private val videos: MutableList<Pair<String, Int>>, // List of pairs (videoKey, postId)
    private val lifecycleOwner: LifecycleOwner,
    private val UserId: Int // Pass userId to the adapter
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    private var currentlyPlayingPlayer: YouTubePlayer? = null

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

                        // Handle other states if needed
                        else -> Unit
                    }
                }
            })
        }

        fun saveInteractionData() {
            val playTime = customPlayer.getPlayTime()
            val replayCount = customPlayer.getReplayCount()
            val IsMuted = customPlayer.getIsMuted()
            val likeState = customPlayer.getLikeState()
            val saveState = customPlayer.getSaveState()
            val commentButtonPressed = customPlayer.wasCommentButtonPressed()
            val commentMade = customPlayer.wasCommentMade()
            val (_, postId) = videos[bindingAdapterPosition]

            // Save this data to the database using Exposed ORM
            transaction {
                UserTrailerInteractions.insert {
                    it[userId] = UserId
                    it[UserTrailerInteractions.postId] = postId
                    it[timeSpent] = playTime
                    it[replayTimes] = replayCount
                    it[isMuted] = IsMuted
                    it[trailerLikeState] = likeState
                    it[trailerSaveState] = saveState
                    it[UserTrailerInteractions.commentButtonPressed] = commentButtonPressed
                    it[UserTrailerInteractions.commentMade] = commentMade
                }
            }
        }
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
        holder.saveInteractionData()
        super.onViewDetachedFromWindow(holder)
    }

    fun addItems(newVideos: List<Pair<String, Int>>) {
        val initialSize = videos.size
        videos.addAll(newVideos)
        notifyItemRangeInserted(initialSize, newVideos.size)
    }
}
