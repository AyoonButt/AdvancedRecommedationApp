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
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.Posts
import com.example.firedatabase_assis.database.UserPostInteractions
import com.example.firedatabase_assis.workers.Post
import com.squareup.picasso.Picasso
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class MyPostAdapter(
    private val context: Context,
    private val movies: List<Post>,
) : RecyclerView.Adapter<MyPostAdapter.PostHolder>() {

    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val UserID =
        sharedPreferences.getInt("userId", -1) // Retrieve userId from SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.post_layout, parent, false)
        return PostHolder(view)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        holder.overview.text = movie.overview
        holder.postId = movie.postId // Set postId for the holder

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
        holder.commentMade = false

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
            updateData(holder, holder.postId) // Log interaction with a timestamp
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
                val commentFragment = CommentFragment(movie.postId)
                transaction.replace(R.id.fragment_container, commentFragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Unregister swipe gesture listener when comment section closes
                commentFragment.view?.addOnAttachStateChangeListener(object :
                    View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {}

                    override fun onViewDetachedFromWindow(v: View) {
                        fragmentContainer.setOnTouchListener(null)
                        holder.commentMade =
                            true // Assuming comment was made if fragment was opened
                    }
                })

                updateData(holder, holder.postId)
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
            updateData(holder, holder.postId)
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
            val timeSpent = holder.viewEndTime - holder.viewStartTime
            updateData(holder, holder.postId, timeSpent)
        }
        super.onViewDetachedFromWindow(holder)
    }

    private fun getCurrentTimestamp(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }


    private fun updateData(holder: PostHolder, mypostId: Int, timeSpent: Long? = null) {
        transaction {
            UserPostInteractions.insert {
                it[userId] = UserID
                it[postId] = mypostId
                it[likeState] = holder.likeState
                it[saveState] = holder.saveState
                it[commentButtonPressed] = holder.commentButtonPressed
                it[commentMade] = holder.commentMade
                it[timestamp] = getCurrentTimestamp()

                // Only log timeSpent if it's provided
                timeSpent?.let { spent ->
                    it[timeSpentOnPost] = spent
                }
            }
        }
    }


    private fun updateLikeCount(postId: Int) {
        transaction {
            Posts.update({ Posts.postId eq postId }) {
                // Use a subquery to safely increment the count
                it[postLikeCount] =
                    (Posts.slice(postLikeCount).select { Posts.postId eq postId }
                        .singleOrNull()?.get(postLikeCount) ?: 0) + 1
            }
        }
    }


    inner class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.poster_image)
        var title: TextView = itemView.findViewById(R.id.movie_caption)
        var overview: TextView = itemView.findViewById(R.id.movie_caption)
        var cardView: CardView = itemView.findViewById(R.id.card_view)
        var heart: ImageView = itemView.findViewById(R.id.heart)
        var insideHeart: ImageView = itemView.findViewById(R.id.insideHeart)
        var comments: ImageView = itemView.findViewById(R.id.comments)
        var saved: ImageView = itemView.findViewById(R.id.saved)

        var viewStartTime: Long = 0
        var viewEndTime: Long = 0

        var postId: Int = 0 // Add this field to store postId
        var likeState: Boolean = false
        var saveState: Boolean = false
        var commentButtonPressed: Boolean = false
        var commentMade: Boolean = false
    }
}
