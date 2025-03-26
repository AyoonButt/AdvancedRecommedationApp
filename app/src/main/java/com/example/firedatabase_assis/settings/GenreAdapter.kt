package com.example.firedatabase_assis.settings


import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.GenreItem


class GenreAdapter(private val genresList: MutableList<GenreItem>) :
    RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    private lateinit var itemTouchHelper: ItemTouchHelper

    fun setItemTouchHelper(touchHelper: ItemTouchHelper) {
        this.itemTouchHelper = touchHelper
    }

    class GenreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewGenreName: TextView = view.findViewById(R.id.textViewGenreName)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.genre_item, parent, false)
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genresList[position]
        holder.textViewGenreName.text = genre.name

        // Start drag only when handle is touched
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && ::itemTouchHelper.isInitialized) {
                itemTouchHelper.startDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = genresList.size
}