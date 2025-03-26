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
import com.example.firedatabase_assis.login_setup.SubscriptionItem


class SubscriptionAdapter(private val subscriptionsList: MutableList<SubscriptionItem>) :
    RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>() {

    private lateinit var itemTouchHelper: ItemTouchHelper

    fun setItemTouchHelper(touchHelper: ItemTouchHelper) {
        this.itemTouchHelper = touchHelper
    }

    class SubscriptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewSubscriptionName: TextView = view.findViewById(R.id.textViewSubscriptionName)
        val dragHandle: ImageView = view.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.subscription_item, parent, false)
        return SubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val subscription = subscriptionsList[position]
        holder.textViewSubscriptionName.text = subscription.name

        // Start drag only when handle is touched
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && ::itemTouchHelper.isInitialized) {
                itemTouchHelper.startDrag(holder)
            }
            false
        }
    }

    override fun getItemCount() = subscriptionsList.size
}