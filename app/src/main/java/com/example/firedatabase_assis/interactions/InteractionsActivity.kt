package com.example.firedatabase_assis.interactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityInteractionsBinding
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Posts
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InteractionsActivity : BaseActivity() {
    private lateinit var binding: ActivityInteractionsBinding
    private lateinit var viewModel: InteractionsViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var gridAdapter: InteractionsGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInteractionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)

        userViewModel = UserViewModel.getInstance(application)
        viewModel = ViewModelProvider(
            this,
            InteractionsViewModelFactory(userViewModel)
        )[InteractionsViewModel::class.java]

        setupToolbar("My Interactions")
        setupRecyclerView()
        setupToggleGroups()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setTitle(title)
        }
        binding.toolbar.setNavigationOnClickListener {
            viewModel.onEvent(InteractionEvent.NavigateBack)
        }
    }

    private fun setupRecyclerView() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val postsApi = retrofit.create(Posts::class.java)

        gridAdapter = InteractionsGridAdapter(
            onItemClick = { itemId ->
                viewModel.onEvent(InteractionEvent.SelectItem(itemId))
            },
            postsApi = postsApi
        )

        binding.interactionsGrid.apply {
            layoutManager = GridLayoutManager(this@InteractionsActivity, 3).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
            adapter = gridAdapter
            setHasFixedSize(true)
            visibility = View.VISIBLE  // Set initial visibility
        }
    }

    private fun setupToggleGroups() {
        binding.interactionToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnLiked.id -> viewModel.onEvent(
                        InteractionEvent.SelectInteractionType(
                            InteractionType.LIKED
                        )
                    )

                    binding.btnSaved.id -> viewModel.onEvent(
                        InteractionEvent.SelectInteractionType(
                            InteractionType.SAVED
                        )
                    )

                    binding.btnCommented.id -> viewModel.onEvent(
                        InteractionEvent.SelectInteractionType(
                            InteractionType.COMMENTED
                        )
                    )
                }
            }
        }

        binding.contentToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnPosts.id -> viewModel.onEvent(
                        InteractionEvent.SelectContentType(
                            ContentType.POSTS
                        )
                    )

                    binding.btnVideos.id -> viewModel.onEvent(
                        InteractionEvent.SelectContentType(
                            ContentType.VIDEOS
                        )
                    )
                }
            }
        }
    }


    private fun setupClickListeners() {
        binding.apply {
            btnLiked.setOnClickListener {
                if (!it.isSelected) {
                    viewModel.onEvent(InteractionEvent.SelectInteractionType(InteractionType.LIKED))
                }
            }
            btnSaved.setOnClickListener {
                if (!it.isSelected) {
                    viewModel.onEvent(InteractionEvent.SelectInteractionType(InteractionType.SAVED))
                }
            }
            btnCommented.setOnClickListener {
                if (!it.isSelected) {
                    viewModel.onEvent(InteractionEvent.SelectInteractionType(InteractionType.COMMENTED))
                }
            }

            btnPosts.setOnClickListener {
                if (!it.isSelected) {
                    viewModel.onEvent(InteractionEvent.SelectContentType(ContentType.POSTS))
                }
            }
            btnVideos.setOnClickListener {
                if (!it.isSelected) {
                    viewModel.onEvent(InteractionEvent.SelectContentType(ContentType.VIDEOS))
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            updateUI(state)
        }

        viewModel.uiEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { uiEvent ->
                handleNavigationEvent(uiEvent)
            }
        }
    }

    private fun updateUI(state: InteractionsState) {
        binding.apply {
            // Update loading state
            loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Update error state
            errorText.visibility = if (state.error != null) View.VISIBLE else View.GONE
            errorText.text = state.error

            // Update content visibility
            interactionsGrid.visibility =
                if (!state.isLoading && state.error == null) View.VISIBLE else View.GONE

            // Update grid content
            if (!state.isLoading && state.error == null) {
                val items = state.items
                println("Submitting items to adapter: ${items.size}")
                println("Item IDs: ${items.map { it.title }}")
                gridAdapter.submitList(items)
            }


            // Update button states
            updateInteractionTypeButtons(state.selectedInteraction)
            updateContentTypeButtons(state.contentType)
        }
    }

    private fun handleNavigationEvent(event: InteractionUiEvent) {
        when (event) {
            is InteractionUiEvent.NavigateToFeed -> {
                if (event.isComment) {
                    startActivity(
                        SingleItemActivity.newIntent(
                            context = this,
                            postDto = event.item,  // Pass the complete PostDto
                            openComments = true
                        )
                    )
                } else {
                    startActivity(
                        FeedActivity.newIntent(
                            context = this,
                            items = viewModel.getCurrentPosts(),  // Pass the current list of PostDto
                            scrollToItem = event.item,  // Pass the complete PostDto to scroll to
                            isVideo = viewModel.getCurrentContentType() == ContentType.VIDEOS,
                            interactionType = viewModel.getCurrentInteractionType().name
                        )
                    )
                }
            }

            InteractionUiEvent.NavigateBack -> finish()
        }
    }


    private fun updateInteractionTypeButtons(selected: InteractionType) {
        binding.apply {
            btnLiked.isSelected = selected == InteractionType.LIKED
            btnSaved.isSelected = selected == InteractionType.SAVED
            btnCommented.isSelected = selected == InteractionType.COMMENTED
        }
    }

    private fun updateContentTypeButtons(selected: ContentType) {
        binding.apply {
            btnPosts.isSelected = selected == ContentType.POSTS
            btnVideos.isSelected = selected == ContentType.VIDEOS
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, InteractionsActivity::class.java)
        }
    }
}