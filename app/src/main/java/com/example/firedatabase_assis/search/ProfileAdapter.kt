package com.example.firedatabase_assis.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.firedatabase_assis.R

class ProfileAdapter(
    private val onItemClick: (Person) -> Unit
) : ListAdapter<Person, ProfileAdapter.ProfileViewHolder>(ProfileDiffCallback()) {

    // Use different view types for different items
    override fun getItemViewType(position: Int): Int {
        // Return the position as view type to ensure each position gets a unique type
        // This prevents view recycling issues with images
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return ProfileViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val person = getItem(position)
        holder.bind(person)
    }

    override fun onViewRecycled(holder: ProfileViewHolder) {
        super.onViewRecycled(holder)
        holder.clearImage()
    }

    class ProfileViewHolder(
        view: View,
        private val onItemClick: (Person) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.profile_image)
        private val textView: TextView = view.findViewById(R.id.name)
        private var currentPersonId: Int = -1
        private val glideRequest: RequestManager = Glide.with(view.context)

        fun bind(person: Person) {
            textView.text = person.name
            currentPersonId = person.id

            // Always clear any existing request first
            clearImage()

            // Then set the new image if available
            if (!person.profile_path.isNullOrEmpty()) {
                val imageUrl = "https://image.tmdb.org/t/p/w185${person.profile_path}"

                glideRequest
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.person_placeholder)
                            .error(R.drawable.person_placeholder)
                    )
                    .circleCrop()
                    .into(imageView)
            } else {
                // Set placeholder for null profile paths
                imageView.setImageResource(R.drawable.person_placeholder)
            }

            itemView.setOnClickListener {
                onItemClick(person)
            }
        }

        fun clearImage() {
            glideRequest.clear(imageView)
            imageView.setImageResource(R.drawable.person_placeholder)
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<Person>() {
        override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean {
            return oldItem == newItem
        }
    }

    // Override setHasStableIds to ensure stable ids
    init {
        setHasStableIds(true)
    }

    // Provide stable IDs based on person's ID
    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    fun cleanup() {
        // Clear all items
        submitList(null)
    }
}