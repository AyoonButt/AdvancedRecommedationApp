package com.example.firedatabase_assis.settings


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.SubscriptionProvider

class ProviderAdapter(private val onProviderSelected: (SubscriptionProvider) -> Unit) :
    RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    private var providersList: List<SubscriptionProvider> = emptyList()

    fun updateProviders(providers: List<SubscriptionProvider>) {
        this.providersList = providers
        notifyDataSetChanged()
    }

    class ProviderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewProviderName: TextView = view.findViewById(R.id.provider_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.provider_item, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providersList[position]
        holder.textViewProviderName.text = provider.providerName

        holder.itemView.setOnClickListener {
            onProviderSelected(provider)
        }
    }

    override fun getItemCount() = providersList.size
}