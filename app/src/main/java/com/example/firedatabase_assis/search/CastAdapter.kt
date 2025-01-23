package com.example.firedatabase_assis.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.R

class CastAdapter(
    private val onItemClick: (CastMember) -> Unit // Pass a lambda for click handling
) : ListAdapter<CastMember, CastAdapter.CastViewHolder>(CastDiffCallback()) {
    private var isActive = true

    fun setActive(active: Boolean) {
        isActive = active
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cast_item, parent, false)
        return CastViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        if (!isActive) return
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<CastMember>?) {
        if (!isActive) return

        val filteredList = list?.filter { member ->
            member.profilePath != null && member.profilePath.isNotEmpty()
        }
        super.submitList(filteredList)
    }

    class CastViewHolder(
        view: View,
        private val onItemClick: (CastMember) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val profileImageView: ImageView = view.findViewById(R.id.profileImage)
        private val nameTextView: TextView = view.findViewById(R.id.actorName)
        private val characterTextView: TextView = view.findViewById(R.id.characterName)
        private val episodeCountTextView: TextView = view.findViewById(R.id.episodeCount)

        fun bind(castMember: CastMember) {
            nameTextView.text = castMember.name
            characterTextView.text = castMember.character

            if (castMember.episodeCount > 1) {
                episodeCountTextView.visibility = View.VISIBLE
                episodeCountTextView.text = "${castMember.episodeCount} episodes"
            } else {
                episodeCountTextView.visibility = View.GONE
            }

            castMember.profilePath?.let { path ->
                if (path.isNotEmpty()) {
                    Glide.with(profileImageView.context)
                        .load("https://image.tmdb.org/t/p/original$path")
                        .circleCrop()
                        .error(R.drawable.profile)  // Add a placeholder
                        .into(profileImageView)
                }
            }

            // Set click listener
            itemView.setOnClickListener {
                onItemClick(castMember)
            }
        }

        fun clear() {
            Glide.with(itemView.context).clear(profileImageView)
        }
    }

    override fun onViewRecycled(holder: CastViewHolder) {
        if (!isActive) return
        super.onViewRecycled(holder)
        try {
            holder.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
