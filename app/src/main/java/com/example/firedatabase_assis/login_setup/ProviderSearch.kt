package com.example.firedatabase_assis.login_setup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R


class ProviderSearch(private var providers: List<Provider>) :
    RecyclerView.Adapter<ProviderSearch.ProviderViewHolder>() {

    private var filteredProviders: List<Provider> = providers

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.provider_item, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = filteredProviders[position]
        holder.bind(provider)
    }

    override fun getItemCount(): Int = filteredProviders.size

    fun filter(query: String) {
        filteredProviders = if (query.isEmpty()) {
            providers
        } else {
            providers.filter { it.provider_name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged() // Notify the adapter to refresh the list
    }

    class ProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val providerNameTextView: TextView = itemView.findViewById(R.id.provider_name)

        fun bind(provider: Provider) {
            providerNameTextView.text = provider.provider_name
        }
    }
}
