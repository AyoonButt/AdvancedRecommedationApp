package com.example.firedatabase_assis.interactions

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ItemInteractionGridBinding
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InteractionsGridAdapter(
    private val onItemClick: (Int) -> Unit,
    private val postsApi: Posts
) : ListAdapter<PostDto, InteractionsGridAdapter.GridViewHolder>(DiffCallback()) {

    private val imageCache = mutableMapOf<Int, String>()
    private var currentPage = 0
    private var isLoading = false
    private var interactionIds: List<Int> = emptyList()

    fun setInteractionIds(ids: List<Int>) {
        interactionIds = ids
        loadNextPage()
    }

    private fun loadNextPage() {
        if (isLoading) return

        CoroutineScope(Dispatchers.IO).launch {
            isLoading = true
            try {
                val response = postsApi.getPagedPostDtos(
                    interactionIds = interactionIds,
                    page = currentPage
                )

                if (response.isSuccessful) {
                    response.body()?.let { posts ->
                        withContext(Dispatchers.Main) {
                            submitList(currentList + posts)
                            currentPage++
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemInteractionGridBinding.inflate(inflater, parent, false)
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)

        // Load more items when reaching end of list
        if (position >= itemCount - 5 && !isLoading) {
            loadNextPage()
        }

        post.posterPath?.let { posterPath ->
            loadImage(posterPath, holder.binding.thumbnail)
        } ?: holder.binding.thumbnail.setImageResource(R.drawable.lotr)

    }

    private fun loadImage(posterPath: String, imageView: ImageView) {
        val baseURL = "https://image.tmdb.org/t/p/original$posterPath"
        Picasso.get()
            .load(baseURL)
            .resize(350, 500)
            .centerCrop()
            .into(imageView)
    }

    inner class GridViewHolder(
        val binding: ItemInteractionGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position).postId?.let { it1 -> onItemClick(it1) }
                }
            }
        }

        fun bind(post: PostDto) {
            binding.thumbnail.setImageResource(R.drawable.lotr)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PostDto>() {
        override fun areItemsTheSame(oldItem: PostDto, newItem: PostDto) =
            oldItem.postId == newItem.postId

        override fun areContentsTheSame(oldItem: PostDto, newItem: PostDto) =
            oldItem == newItem
    }

    fun clearCache() {
        imageCache.clear()
        currentPage = 0
        submitList(null)
    }
}