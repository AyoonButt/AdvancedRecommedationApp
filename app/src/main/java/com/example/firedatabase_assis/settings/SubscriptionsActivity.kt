package com.example.firedatabase_assis.settings


import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.DragEvent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivitySubscriptionsBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Providers
import com.example.firedatabase_assis.postgres.SubscriptionProvider
import com.example.firedatabase_assis.postgres.UserSubscriptionDto
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SubscriptionsActivity : BaseActivity() {
    private lateinit var binding: ActivitySubscriptionsBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var linearLayoutSubscriptions: LinearLayout
    private lateinit var linearLayoutProviders: LinearLayout
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

        linearLayoutSubscriptions = binding.linearLayoutSubscriptions
        linearLayoutProviders = binding.linearLayoutProviders
        editTextAddSubscription = binding.editTextAddSubscription

        setupSubscriptionsList()
        setupSearchProvider()
        setupSaveButton()
        setupToolbar("Subscriptions")
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
            startActivity(intent)
            finish()
        }
    }

    private fun setupSubscriptionsList() {
        lifecycleScope.launch {
            try {
                userViewModel.currentUser.value?.let { currentUser ->
                    val response = providersApi.getUserSubscriptions(currentUser.userId)

                    if (response.isSuccessful) {
                        val subscriptions = response.body() ?: emptyList()
                        subscriptions.sortedBy { it.priority }.forEach { subscription ->
                            val subscriptionView = createSubscriptionView(subscription.providerName)
                            linearLayoutSubscriptions.addView(subscriptionView)
                        }
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

    private fun createSubscriptionView(name: String): View {
        val inflater = LayoutInflater.from(this)
        val subscriptionView =
            inflater.inflate(R.layout.subscription_item, linearLayoutSubscriptions, false)
        val textView = subscriptionView.findViewById<TextView>(R.id.textViewSubscriptionName)
        textView.text = name

        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val swipeThreshold = 100
                    val deltaX = e2.x - e1.x

                    if (deltaX > swipeThreshold) { // Right swipe
                        // Animate the view before removing
                        subscriptionView.animate()
                            .translationX(subscriptionView.width.toFloat())
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                linearLayoutSubscriptions.removeView(subscriptionView)
                            }
                        return true
                    }
                    return false
                }
            })

        subscriptionView.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }

            if (event.action == MotionEvent.ACTION_DOWN) {
                val shadowBuilder = View.DragShadowBuilder(v)
                val dragData = ClipData.newPlainText("", "")

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    v.startDragAndDrop(dragData, shadowBuilder, v, 0)
                } else {
                    @Suppress("DEPRECATION")
                    v.startDrag(dragData, shadowBuilder, v, 0)
                }

                v.visibility = View.INVISIBLE
                true
            } else {
                false
            }
        }

        return subscriptionView
    }

    private fun setupSearchProvider() {
        editTextAddSubscription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterProviders(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editTextAddSubscription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                linearLayoutProviders.visibility = View.VISIBLE
            } else if (editTextAddSubscription.text.isEmpty()) {
                linearLayoutProviders.visibility = View.GONE
            }
        }

        linearLayoutSubscriptions.setOnDragListener { v, event ->
            handleDragEvent(v, event, linearLayoutSubscriptions)
        }
    }

    private fun filterProviders(query: String) {
        if (query.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val response = providersApi.filterProviders(query)
                    if (response.isSuccessful) {
                        val filteredProviders = response.body() ?: emptyList()
                        updateProviders(filteredProviders)
                    }
                } catch (e: Exception) {
                    Log.e("SubscriptionsActivity", "Error filtering providers", e)
                    updateProviders(emptyList())
                }
            }
        } else {
            updateProviders(emptyList())
        }
    }

    private fun updateProviders(providers: List<SubscriptionProvider>) {
        linearLayoutProviders.removeAllViews()
        providers.forEach { provider ->
            val providerView = LayoutInflater.from(this)
                .inflate(R.layout.provider_item, linearLayoutProviders, false)
            val textViewProviderName = providerView.findViewById<TextView>(R.id.provider_name)
            textViewProviderName.text = provider.providerName
            providerView.setOnClickListener {
                val subscriptionView = createSubscriptionView(provider.providerName)
                linearLayoutSubscriptions.addView(subscriptionView)
                editTextAddSubscription.setText("")
                linearLayoutProviders.removeAllViews()
            }
            linearLayoutProviders.addView(providerView)
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    userViewModel.currentUser.value?.let { currentUser ->
                        // Get ordered provider names from layout
                        val orderedNames =
                            getOrderOfSubscriptionItemNames(linearLayoutSubscriptions)

                        // Get provider IDs for these names
                        val providerIdsResponse = providersApi.getProviderIdsByNames(orderedNames)
                        if (!providerIdsResponse.isSuccessful) {
                            throw Exception("Failed to get provider IDs")
                        }

                        val providerIds = providerIdsResponse.body() ?: emptyList()

                        // Create subscription DTOs with priorities
                        val subscriptions = providerIds.mapIndexed { index, providerId ->
                            UserSubscriptionDto(
                                userId = currentUser.userId,
                                providerId = providerId,
                                providerName = orderedNames[index],
                                priority = index + 1
                            )
                        }

                        // Update subscriptions
                        val updateResponse = providersApi.updateUserSubscriptions(
                            currentUser.userId,
                            subscriptions
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

    // Helper functions from SetupActivity
    private fun handleDragEvent(v: View, event: DragEvent, targetLayout: LinearLayout): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                v.invalidate()
                true
            }

            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> {
                v.invalidate()
                true
            }

            DragEvent.ACTION_DROP -> {
                val view = event.localState as View
                val owner = view.parent as LinearLayout
                owner.removeView(view)
                targetLayout.addView(view)
                view.visibility = View.VISIBLE
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                val view = event.localState as View
                view.visibility = View.VISIBLE
                v.invalidate()
                true
            }

            else -> false
        }
    }

    private fun getOrderOfSubscriptionItemNames(layout: LinearLayout): List<String> {
        val sequence = mutableListOf<String>()
        for (i in 0 until layout.childCount) {
            val view = layout.getChildAt(i)
            val textView = view.findViewById<TextView>(R.id.textViewSubscriptionName)
            textView?.let { sequence.add(it.text.toString()) }
        }
        return sequence
    }

    private suspend fun getProviderIds(names: List<String>): List<Int> {
        val response = providersApi.getProviderIdsByNames(names)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }
}