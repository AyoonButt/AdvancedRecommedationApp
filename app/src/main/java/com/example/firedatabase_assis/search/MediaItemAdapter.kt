package com.example.firedatabase_assis.search

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.R

class MediaItemAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val isRecommendation: Boolean
) : ListAdapter<MediaItem, RecyclerView.ViewHolder>(MediaItemDiffCallback()) {

    companion object {
        const val VIEW_TYPE_SEARCH = 1
        const val VIEW_TYPE_RECOMMENDATION = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (isRecommendation) VIEW_TYPE_RECOMMENDATION else VIEW_TYPE_SEARCH
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SEARCH -> {
                val view = inflater.inflate(R.layout.item_movie_tv, parent, false)
                SearchViewHolder(view)
            }

            VIEW_TYPE_RECOMMENDATION -> {
                val view = inflater.inflate(R.layout.item_recommendation, parent, false)
                RecommendationViewHolder(view)
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SearchViewHolder -> holder.bind(item, onItemClick)
            is RecommendationViewHolder -> holder.bind(item, onItemClick)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is SearchViewHolder -> holder.unbind()
            is RecommendationViewHolder -> holder.unbind()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is SearchViewHolder -> holder.onViewDestroyed()
            is RecommendationViewHolder -> holder.onViewDestroyed()
        }
    }

    fun cleanup() {
        submitList(null)
    }

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.poster_image)
        private val titleView: TextView = view.findViewById(R.id.title)

        fun bind(item: MediaItem, onItemClick: (MediaItem) -> Unit) {
            titleView.text = item.title
            Glide.with(imageView.context)
                .load("https://image.tmdb.org/t/p/original${item.poster_path}")
                .into(imageView)

            itemView.setOnClickListener { onItemClick(item) }
        }

        private var isViewHolderDestroyed = false

        fun unbind() {
            try {
                // Only clear images if not destroyed
                if (!isViewHolderDestroyed) {
                    Glide.with(itemView.context).clear(itemView)
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }

        fun onViewDestroyed() {
            isViewHolderDestroyed = true
        }
    }

    class RecommendationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.poster_image)
        private val titleView: TextView = view.findViewById(R.id.title)
        private var isViewHolderDestroyed = false

        fun bind(item: MediaItem, onItemClick: (MediaItem) -> Unit) {
            titleView.text = item.title
            Glide.with(imageView.context)
                .load("https://image.tmdb.org/t/p/original${item.poster_path}") // Smaller image size
                .into(imageView)

            itemView.setOnClickListener { onItemClick(item) }
        }

        fun unbind() {
            Glide.with(imageView.context).clear(imageView)
            imageView.setImageDrawable(null)
            titleView.text = ""
            itemView.setOnClickListener(null)
        }

        fun onViewDestroyed() {
            isViewHolderDestroyed = true
        }
    }

    private class MediaItemDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id && oldItem.mediaType == newItem.mediaType
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }

    class ItemSpacingDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            super.getItemOffsets(outRect, view, parent, state)
            outRect.top = space // Space above the item
            outRect.bottom = space // Space below the item
            outRect.left = space // Space to the left of the item
            outRect.right = space // Space to the right of the item
        }
    }

}

