package com.example.firedatabase_assis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(private val posts: MutableList<Post>, private val activity: AppCompatActivity) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.titleTextView.text = post.title
        holder.contentTextView.text = post.content
        holder.authorTextView.text = "by ${post.author} in ${post.community}"
        holder.upvotesTextView.text = post.upvotes.toString()
        holder.downvotesTextView.text = post.downvotes.toString()

        // Set up RecyclerView for comments
        holder.commentsRecyclerView.layoutManager = LinearLayoutManager(activity)
        holder.commentsRecyclerView.adapter = CommentAdapter(post.comments)

        // Handle new comment addition
        holder.addCommentButton.setOnClickListener {
            val commentText = holder.newCommentText.text.toString()
            if (commentText.isNotEmpty()) {
                post.comments.add(Comment("Current User", commentText))
                holder.newCommentText.text.clear()
                holder.commentsRecyclerView.adapter?.notifyDataSetChanged()
                Toast.makeText(activity, "Posted!", Toast.LENGTH_SHORT).show()
            }
        }

        holder.upvoteButton.setOnClickListener {
            if (!post.upvoted) {
                post.upvotes++
                holder.upvotesTextView.text = post.upvotes.toString()
                post.upvoted = true
                holder.upvoteButton.setImageResource(R.drawable.ic_upvote) // Change icon to indicate upvote
                Toast.makeText(activity, "Upvoted!", Toast.LENGTH_SHORT).show()

                // If downvoted, remove downvote
                if (post.downvoted) {
                    post.downvotes--
                    holder.downvotesTextView.text = post.downvotes.toString()
                    post.downvoted = false
                    holder.downvoteButton.setImageResource(R.drawable.ic_downvote) // Change icon to indicate un-downvote
                }
            } else {
                post.upvotes--
                holder.upvotesTextView.text = post.upvotes.toString()
                post.upvoted = false
                holder.upvoteButton.setImageResource(R.drawable.ic_upvote) // Change icon to indicate un-upvote
                Toast.makeText(activity, "Upvote removed!", Toast.LENGTH_SHORT).show()
            }
        }

        holder.downvoteButton.setOnClickListener {
            if (!post.downvoted) {
                post.downvotes++
                holder.downvotesTextView.text = post.downvotes.toString()
                post.downvoted = true
                holder.downvoteButton.setImageResource(R.drawable.ic_downvote) // Change icon to indicate downvote
                Toast.makeText(activity, "Downvoted!", Toast.LENGTH_SHORT).show()

                // If upvoted, remove upvote
                if (post.upvoted) {
                    post.upvotes--
                    holder.upvotesTextView.text = post.upvotes.toString()
                    post.upvoted = false
                    holder.upvoteButton.setImageResource(R.drawable.ic_upvote) // Change icon to indicate un-upvote
                }
            } else {
                post.downvotes--
                holder.downvotesTextView.text = post.downvotes.toString()
                post.downvoted = false
                holder.downvoteButton.setImageResource(R.drawable.ic_downvote) // Change icon to indicate un-downvote
                Toast.makeText(activity, "Downvote removed!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun getItemCount() = posts.size
    fun addPost(post: Post) {
        posts.add(0, post)  // Adds the new post at the beginning of the list
        notifyItemInserted(0)
        Toast.makeText(activity, "Posted!", Toast.LENGTH_SHORT).show()
    }


    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.post_title)
        val contentTextView: TextView = itemView.findViewById(R.id.post_content)
        val authorTextView: TextView = itemView.findViewById(R.id.post_author)
        val upvotesTextView: TextView = itemView.findViewById(R.id.post_upvotes)
        val downvotesTextView: TextView = itemView.findViewById(R.id.post_downvotes)
        val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comment_list)
        val newCommentText: EditText = itemView.findViewById(R.id.new_comment_text)
        val addCommentButton: Button = itemView.findViewById(R.id.add_comment_button)
        val upvoteButton: ImageView = itemView.findViewById(R.id.upvote_button)
        val downvoteButton: ImageView = itemView.findViewById(R.id.downvote_button)
    }
}