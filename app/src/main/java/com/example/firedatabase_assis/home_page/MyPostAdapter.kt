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
import com.example.firedatabase_assis.database.CommentDao
import com.squareup.picasso.Picasso


class MyPostAdapter(
    private val context: Context,
    private val movies: List<Post>,
    private val commentDao: CommentDao
) : RecyclerView.Adapter<MyPostAdapter.PostHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.post_layout, parent, false)
        return PostHolder(view)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        holder.overview.text = movie.overview

        // Load image using Picasso
        val baseURL = "https://image.tmdb.org/t/p/w185${movie.posterPath}"
        Picasso.get().load(baseURL).into(holder.imageView)

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
                        // Hide insideHeart after animations complete
                        holder.insideHeart.visibility = View.INVISIBLE
                    }
                })

                // Start the combined animation
                holder.insideHeart.startAnimation(animationSet)
            }
        })

        // Add single click listener to heart ImageView
        holder.heart.setOnClickListener {
            if (holder.heart.tag == "liked") {
                holder.heart.setImageResource(R.drawable.heart_outline)
                holder.heart.tag = "unliked"
            } else {
                holder.heart.setImageResource(R.drawable.heart_red)
                holder.heart.tag = "liked"
            }
        }

        // Set up comments section
        holder.comments.setOnClickListener {
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
                val commentFragment = CommentFragment(movie.id, commentDao)
                transaction.replace(R.id.fragment_container, commentFragment)
                transaction.addToBackStack(null)
                transaction.commit()

                // Unregister swipe gesture listener when comment section closes
                commentFragment.view?.addOnAttachStateChangeListener(object :
                    View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {}

                    override fun onViewDetachedFromWindow(v: View) {
                        fragmentContainer.setOnTouchListener(null)
                    }
                })
            }
        }


        var isSaved = false
        holder.saved.setOnClickListener {
            isSaved = !isSaved
            if (isSaved) {
                holder.saved.setImageResource(R.drawable.icon_bookmark_filled)
            } else {
                holder.saved.setImageResource(R.drawable.icon_bookmark_unfilled)
            }
        }
    }

    override fun getItemCount(): Int {
        return movies.size
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
    }
}


