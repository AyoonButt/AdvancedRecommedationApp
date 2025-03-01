package com.example.firedatabase_assis.home_page

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.InteractionStates
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.PostInteractions
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.UserPostInteractionDto
import com.example.firedatabase_assis.search.SearchViewModel
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyPostAdapter(
    private val context: Context,
    private val movies: MutableList<PostDto>,
    private val userViewModel: UserViewModel,
    private val searchViewModel: SearchViewModel

) : RecyclerView.Adapter<MyPostAdapter.PostHolder>() {

    val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val postsService = retrofit.create(Posts::class.java)
    private val postInteractionsService = retrofit.create(PostInteractions::class.java)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.post_layout, parent, false)
        return PostHolder(view)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        holder.overview.text = movie.overview
        holder.postId = movie.postId ?: 0 // Set postId for the holder


        // Load image using Picasso
        val baseURL = "https://image.tmdb.org/t/p/original${movie.posterPath}"
        Picasso.get()
            .load(baseURL)
            .resize(350, 500)
            .centerCrop()
            .into(holder.imageView)

        // Initialize interaction states
        holder.likeState = holder.heart.tag == "liked"
        holder.saveState = holder.saved.tag == "saved"
        holder.commentButtonPressed = false

        CoroutineScope(Dispatchers.IO).launch {
            val userId = userViewModel.currentUser.value?.userId ?: return@launch
            val states = getStates(userId, holder.postId)

            withContext(Dispatchers.Main) {
                // Initialize interaction states from backend
                holder.likeState = states.isLiked
                holder.saveState = states.isSaved

                // Update UI to reflect states
                holder.heart.setImageResource(
                    if (states.isLiked) R.drawable.heart_red else R.drawable.heart_outline
                )
                holder.heart.tag = if (states.isLiked) "liked" else "unliked"

                holder.saved.setImageResource(
                    if (states.isSaved) R.drawable.icon_bookmark_filled else R.drawable.icon_bookmark_unfilled
                )
                holder.saved.tag = if (states.isSaved) "saved" else "not_saved"
            }
        }

        // Add double click listener to card view
        holder.cardView.setOnClickListener(object : DoubleClickListener() {
            override fun onDoubleClick(v: View?) {
                // Apply animations regardless of the current state
                holder.heart.setImageResource(R.drawable.heart_red)
                holder.heart.tag = "liked"
                val zoomInAnim = AnimationUtils.loadAnimation(context, R.anim.zoom_in)
                val zoomOutAnim = AnimationUtils.loadAnimation(context, R.anim.zoom_out)

                // Combine both animations into an AnimationSet
                val animationSet = AnimationSet(true)
                animationSet.addAnimation(zoomInAnim)
                animationSet.addAnimation(zoomOutAnim)

                holder.insideHeart.setImageResource(R.drawable.heart_white)
                holder.insideHeart.visibility = View.VISIBLE

                // Set AnimationListener to handle animation end
                animationSet.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        holder.insideHeart.visibility = View.INVISIBLE
                    }
                })

                holder.insideHeart.startAnimation(animationSet)
            }
        })

        // Add single click listener to heart ImageView
        holder.heart.setOnClickListener {
            holder.likeState = holder.heart.tag != "liked"
            if (holder.likeState) {
                holder.heart.setImageResource(R.drawable.heart_red)
                holder.heart.tag = "liked"
                updateLikeCount(holder.postId) // Increment like count in database
            } else {
                holder.heart.setImageResource(R.drawable.heart_outline)
                holder.heart.tag = "unliked"
            }
        }

        // Set up comments section
        holder.comments.setOnClickListener {
            holder.commentButtonPressed = true
            val activity = context as AppCompatActivity
            val fragmentManager = activity.supportFragmentManager
            val fragmentContainer = activity.findViewById<View>(R.id.fragment_container)
            if (fragmentContainer != null) {
                fragmentContainer.visibility = View.VISIBLE
                // Set the top margin of the fragment container layout to 0
                val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = 0
                fragmentContainer.layoutParams = layoutParams

                val transaction = fragmentManager.beginTransaction()
                val commentFragment =
                    movie.postId?.let { it1 -> CommentFragment(it1, commentType = "post") }
                if (commentFragment != null) {
                    transaction.replace(R.id.fragment_container, commentFragment)
                }
                transaction.addToBackStack(null)
                transaction.commit()

                // Unregister swipe gesture listener when comment section closes
                if (commentFragment != null) {
                    commentFragment.view?.addOnAttachStateChangeListener(object :
                        View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {}

                        override fun onViewDetachedFromWindow(v: View) {
                            fragmentContainer.setOnTouchListener(null)
                        }
                    })
                }

            }
        }

        var isSaved = false
        holder.saved.setOnClickListener {
            isSaved = !isSaved
            holder.saveState = isSaved
            if (isSaved) {
                holder.saved.setImageResource(R.drawable.icon_bookmark_filled)
                holder.saved.tag = "saved"
            } else {
                holder.saved.setImageResource(R.drawable.icon_bookmark_unfilled)
                holder.saved.tag = "not_saved"
            }
        }

        holder.info.setOnClickListener {
            val isMovie = when (movie.type.lowercase()) {
                "movie" -> true
                else -> false
            }
            searchViewModel.navigate(
                SearchViewModel.NavigationState.ShowPoster(
                    movie.tmdbId, isMovie
                )
            )
        }

    }


    override fun getItemCount(): Int {
        return movies.size
    }

    override fun onViewAttachedToWindow(holder: PostHolder) {
        super.onViewAttachedToWindow(holder)
        holder.viewStartTime = System.currentTimeMillis()
    }

    override fun onViewDetachedFromWindow(holder: PostHolder) {
        holder.viewEndTime = System.currentTimeMillis()
        if (holder.viewStartTime != 0L) {
            updateData(holder, holder.postId)
        }
        super.onViewDetachedFromWindow(holder)
    }

    private fun updateData(
        holder: PostHolder, myPostId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {


            val userEntity = userViewModel.currentUser.value

            if (userEntity != null) {

                val interactionData = UserPostInteractionDto(
                    interactionId = 0,
                    userId = userEntity.userId,
                    postId = myPostId,
                    startTimestamp = holder.viewStartTime.toString(),
                    endTimestamp = holder.viewEndTime.toString(),
                    likeState = holder.likeState,
                    saveState = holder.saveState,
                    commentButtonPressed = holder.commentButtonPressed,
                )

                // Call the API to save interaction data
                val response =
                    interactionData.let { postInteractionsService.saveInteractionData(it) }
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        if (response.isSuccessful) {
                            println("Interaction data saved successfully.")
                        } else {
                            println(
                                "Failed to save interaction data: ${
                                    response.errorBody()?.string()
                                }"
                            )
                        }
                    }
                }
            } else {
                println("User or post data not available.")
            }
        }
    }

    fun updateData(newData: List<PostDto>) {
        // Clear and update with a safe pattern
        val oldSize = movies.size
        movies.clear()
        notifyItemRangeRemoved(0, oldSize)

        movies.addAll(newData)
        notifyItemRangeInserted(0, newData.size)
    }


    private fun updateLikeCount(postId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postsService.updateLikeCount(postId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun getStates(userId: Int, postId: Int): InteractionStates {
        return try {
            val response = postInteractionsService.getPostInteractionStates(userId, postId)
            if (response.isSuccessful) {
                response.body() ?: InteractionStates()
            } else {
                InteractionStates()  // Default to false states if request fails
            }
        } catch (e: Exception) {
            println("Error getting interaction states: ${e.message}")
            InteractionStates()  // Default to false states on error
        }
    }


    // Force save function without throttling for activity lifecycle events
    private fun saveInteraction(
        holder: PostHolder,
        currentTime: Long = System.currentTimeMillis()
    ) {
        holder.viewEndTime = currentTime
        updateData(holder, holder.postId)
        holder.lastInteractionUpdate = currentTime
    }

    // Modified to force save all visible interactions
    fun saveAllVisibleInteractions(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        val currentTime = System.currentTimeMillis()

        for (i in firstVisible..lastVisible) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) as? PostHolder ?: continue
            saveInteraction(holder, currentTime)
        }
    }


    inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.poster_image)
        var title: TextView = itemView.findViewById(R.id.title)
        var overview: TextView = itemView.findViewById(R.id.movie_caption)
        var cardView: CardView = itemView.findViewById(R.id.card_view)
        var heart: ImageView = itemView.findViewById(R.id.heart)
        var insideHeart: ImageView = itemView.findViewById(R.id.insideHeart)
        var comments: ImageView = itemView.findViewById(R.id.comments)
        var saved: ImageView = itemView.findViewById(R.id.saved)
        var info: ImageView = itemView.findViewById(R.id.info)

        var viewStartTime: Long = 0
        var viewEndTime: Long = 0

        var postId: Int = 0 // Add this field to store postId
        var likeState: Boolean = false
        var saveState: Boolean = false
        var commentButtonPressed: Boolean = false

        var lastInteractionUpdate: Long = 0

    }
}