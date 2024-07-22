package com.example.firedatabase_assis.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R

class SearchAdapter(private val results: List<Result>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_PERSON = 1
        private const val VIEW_TYPE_MOVIE_TV = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (results[position]) {
            is Result.Person -> VIEW_TYPE_PERSON
            is Result.Movie, is Result.TvShow -> VIEW_TYPE_MOVIE_TV
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PERSON -> {
                val view = inflater.inflate(R.layout.item_person, parent, false)
                PersonViewHolder(view)
            }

            VIEW_TYPE_MOVIE_TV -> {
                val view = inflater.inflate(R.layout.item_movie_tv, parent, false)
                MovieTvViewHolder(view)
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PersonViewHolder -> holder.bind(results[position] as Result.Person)
            is MovieTvViewHolder -> {
                if (results[position] is Result.Movie) {
                    holder.bindMovie(results[position] as Result.Movie)
                } else {
                    holder.bindTvShow(results[position] as Result.TvShow)
                }
            }
        }
    }

    override fun getItemCount(): Int = results.size

    class PersonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(person: Result.Person) {
            // Bind person data to UI
        }
    }

    class MovieTvViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindMovie(movie: Result.Movie) {
            // Bind movie data to UI
        }

        fun bindTvShow(tvShow: Result.TvShow) {
            // Bind TV show data to UI
        }
    }
}
