package com.example.firedatabase_assis.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.R

class ProfileAdapter(private var profiles: List<Person>) :
    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.profile_image)
        val textView: TextView = view.findViewById(R.id.name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val profile = profiles[position]
        holder.textView.text = profile.name

        // Use Glide to load the circular profile image
        Glide.with(holder.imageView.context)
            .load("https://image.tmdb.org/t/p/w45${profile.profile_path}")
            .circleCrop() // This ensures the image is circular
            .into(holder.imageView)
    }

    fun updateData(newProfiles: List<Person>) {
        profiles = newProfiles
        notifyDataSetChanged()
    }

    fun clearData() {
        profiles = emptyList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = profiles.size
}
