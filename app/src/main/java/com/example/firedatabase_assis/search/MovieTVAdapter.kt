package com.example.firedatabase_assis.search

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.firedatabase_assis.R

class MovieTVAdapter(
    private val onItemClick: (Any) -> Unit
) : ListAdapter<Any, MovieTVAdapter.MovieViewHolder>(MovieTVDiffCallback()) {

    private var isItemClicked = false

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.poster_image)
        val titleView: TextView = view.findViewById(R.id.title)

        fun bind(item: Any, onItemClick: (Any) -> Unit) {
            val (posterPath, title) = when (item) {
                is Movie -> item.poster_path to item.title
                is TV -> item.poster_path to item.name
                else -> null to ""
            }

            titleView.text = title

            posterPath?.let { path ->
                Glide.with(imageView.context)
                    .load("https://image.tmdb.org/t/p/original$path")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView)
            }

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(item)
                }
            }
        }

        fun unbind() {
            Glide.with(imageView.context).clear(imageView)
            itemView.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        return MovieViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_movie_tv, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        try {
            val item = getItem(position)
            holder.bind(item, onItemClick)
        } catch (e: Exception) {
            Log.e("MovieTVAdapter", "Error binding view holder at position: $position", e)
        }
    }

    override fun onViewRecycled(holder: MovieViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun submitData(movies: List<Movie>, tvShows: List<TV>) {
        val newList = buildList {
            addAll(movies.filter { !it.poster_path.isNullOrEmpty() })
            addAll(tvShows.filter { !it.poster_path.isNullOrEmpty() })
        }.sortedByDescending { item ->
            when (item) {
                is Movie -> item.popularity
                is TV -> item.popularity
                else -> 0.0
            }
        }
        submitList(newList)
    }
}

private class MovieTVDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Movie && newItem is Movie -> oldItem.id == newItem.id
            oldItem is TV && newItem is TV -> oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Movie && newItem is Movie ->
                oldItem.id == newItem.id &&
                        oldItem.title == newItem.title &&
                        oldItem.poster_path == newItem.poster_path

            oldItem is TV && newItem is TV ->
                oldItem.id == newItem.id &&
                        oldItem.name == newItem.name &&
                        oldItem.poster_path == newItem.poster_path

            oldItem is Person && newItem is Person ->
                oldItem.id == newItem.id &&
                        oldItem.name == newItem.name &&
                        oldItem.profile_path == newItem.profile_path

            else -> false
        }
    }
}