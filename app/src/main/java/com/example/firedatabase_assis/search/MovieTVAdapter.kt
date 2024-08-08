package com.example.firedatabase_assis.search

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.firedatabase_assis.R


class MovieTVAdapter(
    private val movies: List<Movie>,
    private val tvShows: List<TV>
) : RecyclerView.Adapter<MovieTVAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.poster_image)
        val titleView: TextView = view.findViewById(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_tv, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val item =
            if (position < tvShows.size) tvShows[position] else movies[position - tvShows.size]

        val posterPath = when (item) {
            is Movie -> item.poster_path
            is TV -> item.poster_path
            else -> null
        }

        val title = when (item) {
            is Movie -> item.title
            is TV -> item.name
            else -> ""
        }

        // Load image using Glide
        Glide.with(holder.imageView.context)
            .load("https://image.tmdb.org/t/p/original${posterPath}") // Using a higher resolution
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(100, 150)
            .into(holder.imageView)

        holder.titleView.text = title

        holder.imageView.setOnClickListener {
            val activity = holder.itemView.context as AppCompatActivity
            val viewModel = ViewModelProvider(activity)[SearchViewModel::class.java]
            viewModel.setSelectedItem(item)

            // Log the ViewModel's selected item after setting it
            Log.d("MovieTVAdapter", "ViewModel contains: ${viewModel.selectedItem.value}")

            // Instantiate the PosterFragment
            val posterFragment = PosterFragment()

            // Replace the fragment
            val fragmentManager = activity.supportFragmentManager
            val fragmentContainer = activity.findViewById<View>(R.id.container)
            if (fragmentContainer != null) {
                fragmentContainer.visibility = View.VISIBLE
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.container, posterFragment)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }
    }

    override fun getItemCount(): Int = movies.size + tvShows.size
}
