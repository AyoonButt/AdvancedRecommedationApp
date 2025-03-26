package com.example.firedatabase_assis.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.GenreEntity

class GenreSearchAdapter(private val onGenreSelected: (GenreEntity) -> Unit) :
    RecyclerView.Adapter<GenreSearchAdapter.SearchViewHolder>() {

    private var genresList: List<GenreEntity> = emptyList()

    fun updateGenres(genres: List<GenreEntity>) {
        this.genresList = genres
        notifyDataSetChanged()
    }

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewGenreName: TextView = view.findViewById(R.id.genres_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.genre_search_item, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val genre = genresList[position]
        holder.textViewGenreName.text = genre.genreName

        holder.itemView.setOnClickListener {
            onGenreSelected(genre)
        }
    }

    override fun getItemCount() = genresList.size
}