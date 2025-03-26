package com.example.firedatabase_assis.settings

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivitySubscriptionsBinding
import com.example.firedatabase_assis.login_setup.SubscriptionItem
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Providers
import com.example.firedatabase_assis.postgres.UserSubscriptionDto
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SubscriptionsActivity : BaseActivity() {
    private lateinit var binding: ActivitySubscriptionsBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var subscriptionsAdapter: SubscriptionAdapter
    private lateinit var providersAdapter: ProviderAdapter
    private lateinit var subscriptionsList: MutableList<SubscriptionItem>
    private lateinit var editTextAddSubscription: EditText

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val providersApi: Providers = retrofit.create(Providers::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)
        ActivityNavigationHelper.setLastOpenedSettingsActivity(this::class.java)

        userViewModel = UserViewModel.getInstance(application)
        subscriptionsList = mutableListOf()

        editTextAddSubscription = binding.editTextAddSubscription

        setupToolbar("Subscriptions")
        setupRecyclerViews()
        loadUserSubscriptions()
        setupSearchProvider()
        setupSaveButton()
    }

    private fun setupRecyclerViews() {
        // Setup subscriptions RecyclerView
        val recyclerViewSubscriptions = binding.recyclerViewSubscriptions
        recyclerViewSubscriptions.layoutManager = LinearLayoutManager(this)
        subscriptionsAdapter = SubscriptionAdapter(subscriptionsList)
        recyclerViewSubscriptions.adapter = subscriptionsAdapter

        // Setup ItemTouchHelper for drag-and-drop and swipe
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition

                // Update the data
                val item = subscriptionsList.removeAt(fromPos)
                subscriptionsList.add(toPos, item)
                subscriptionsAdapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                subscriptionsList.removeAt(position)
                subscriptionsAdapter.notifyItemRemoved(position)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerViewSubscriptions)
        subscriptionsAdapter.setItemTouchHelper(itemTouchHelper)

        // Setup providers RecyclerView
        val recyclerViewProviders = binding.recyclerViewProviders
        recyclerViewProviders.layoutManager = LinearLayoutManager(this)
        providersAdapter = ProviderAdapter { provider ->
            // Add the selected provider to user's subscriptions
            if (!subscriptionsList.any { it.name == provider.providerName }) {
                provider.providerId?.let { SubscriptionItem(it, provider.providerName) }
                    ?.let { subscriptionsList.add(it) }
                subscriptionsAdapter.notifyItemInserted(subscriptionsList.size - 1)
                editTextAddSubscription.setText("")
                binding.recyclerViewProviders.visibility = View.GONE
            }
        }
        recyclerViewProviders.adapter = providersAdapter
    }

    private fun loadUserSubscriptions() {
        lifecycleScope.launch {
            try {
                userViewModel.currentUser.value?.let { currentUser ->
                    val response = providersApi.getUserSubscriptions(currentUser.userId)

                    if (response.isSuccessful) {
                        val subscriptions = response.body() ?: emptyList()
                        subscriptionsList.clear()

                        subscriptions.sortedBy { it.priority }.forEach { subscription ->
                            subscriptionsList.add(
                                SubscriptionItem(
                                    subscription.providerId,
                                    subscription.providerName
                                )
                            )
                        }

                        subscriptionsAdapter.notifyDataSetChanged()
                    } else {
                        throw Exception("Failed to fetch subscriptions")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SubscriptionsActivity,
                    "Error loading subscriptions: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setTitle(title)
        }
        binding.toolbar.setNavigationOnClickListener {
            // Navigate back to SettingsActivity
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityNavigationHelper.removeLastOpenedSettingsActivity()
            startActivity(intent)
            finish()
        }
    }

    private fun setupSearchProvider() {
        editTextAddSubscription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    binding.recyclerViewProviders.visibility = View.VISIBLE
                    filterProviders(query)
                } else {
                    binding.recyclerViewProviders.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterProviders(query: String) {
        lifecycleScope.launch {
            try {
                val response = providersApi.filterProviders(query)
                if (response.isSuccessful) {
                    val filteredProviders = response.body() ?: emptyList()
                    providersAdapter.updateProviders(filteredProviders)
                } else {
                    providersAdapter.updateProviders(emptyList())
                }
            } catch (e: Exception) {
                Log.e("SubscriptionsActivity", "Error filtering providers", e)
                providersAdapter.updateProviders(emptyList())
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    userViewModel.currentUser.value?.let { currentUser ->
                        val userSubscriptions =
                            subscriptionsList.mapIndexed { index, subscriptionItem ->
                                UserSubscriptionDto(
                                    userId = currentUser.userId,
                                    providerId = subscriptionItem.id,
                                    providerName = subscriptionItem.name,
                                    priority = index + 1
                                )
                            }

                        val updateResponse = providersApi.updateUserSubscriptions(
                            currentUser.userId,
                            userSubscriptions
                        )

                        if (updateResponse.isSuccessful) {
                            Toast.makeText(
                                this@SubscriptionsActivity,
                                "Subscriptions updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            throw Exception("Failed to update subscriptions")
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SubscriptionsActivity,
                        "Error updating subscriptions: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}