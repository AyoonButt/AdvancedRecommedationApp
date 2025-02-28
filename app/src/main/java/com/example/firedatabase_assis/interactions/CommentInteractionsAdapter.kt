package com.example.firedatabase_assis.interactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.databinding.CommentInteractionCardBinding
import com.squareup.picasso.Picasso

class CommentInteractionsAdapter(
    private val onItemClick: (PostWithComments) -> Unit
) : ListAdapter<PostWithComments, CommentInteractionsAdapter.ViewHolder>(
    PostWithCommentsDiffCallback()
) {

    inner class ViewHolder(private val binding: CommentInteractionCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(postWithComments: PostWithComments) {
            val post = postWithComments.post
            binding.apply {
                // Bind post details
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/original${post.posterPath}")
                    .into(posterImage)
                title.text = post.title

                // Bind comments
                commentsContainer.removeAllViews()

                postWithComments.comments.forEach { comment ->
                    val commentView = InteractionCommentView(itemView.context).apply {
                        setComment(comment) { clickedComment ->
                            // Create new PostWithComments here with the clicked comment
                            onItemClick(postWithComments.copy(comments = listOf(clickedComment)))
                        }
                    }
                    commentsContainer.addView(commentView)
                }

                // Set card click listener (passes all comments)
                root.setOnClickListener { view ->
                    // Only trigger if the click wasn't on a comment
                    if (view == root) {
                        onItemClick(postWithComments)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CommentInteractionCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostWithCommentsDiffCallback : DiffUtil.ItemCallback<PostWithComments>() {
        override fun areItemsTheSame(
            oldItem: PostWithComments,
            newItem: PostWithComments
        ): Boolean {
            return oldItem.post.postId == newItem.post.postId
        }

        override fun areContentsTheSame(
            oldItem: PostWithComments,
            newItem: PostWithComments
        ): Boolean {
            return oldItem == newItem
        }
    }
}