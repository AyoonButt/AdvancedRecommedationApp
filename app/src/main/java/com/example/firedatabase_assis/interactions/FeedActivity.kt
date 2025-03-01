package com.example.firedatabase_assis.interactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firedatabase_assis.BaseActivity
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.ActivityFeedBinding
import com.example.firedatabase_assis.explore.VideoAdapter
import com.example.firedatabase_assis.home_page.MyPostAdapter
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.search.SearchViewModel
import com.example.firedatabase_assis.settings.ActivityNavigationHelper

class FeedActivity : BaseActivity() {
    private lateinit var binding: ActivityFeedBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var feedViewModel: FeedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBottomNavigation(R.id.bottom_menu_settings)
        ActivityNavigationHelper.setLastOpenedSettingsActivity(this::class.java)



        userViewModel = UserViewModel.getInstance(application)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        feedViewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        val scrollToItemId = intent.getIntExtra(SCROLL_TO_ITEM_ID, -1)
        val isVideo = intent.getBooleanExtra(IS_VIDEO, false)
        val itemIds = intent.getIntegerArrayListExtra(ITEM_IDS) ?: arrayListOf()
        val interactionType = intent.getStringExtra(INTERACTION_TYPE) ?: "LIKED"

        // Set toolbar title based on interaction type and content type
        val title = when (interactionType) {
            "LIKED" -> if (isVideo) "Liked Trailers" else "Liked Posts"
            "SAVED" -> if (isVideo) "Saved Trailers" else "Saved Posts"
            else -> if (isVideo) "Trailers" else "Posts"
        }
        setupToolbar(title)

        setupRecyclerView()
        feedViewModel.loadFeedItems(itemIds, isVideo)
        observeViewModel(scrollToItemId, isVideo)
    }

    private fun observeViewModel(scrollToItemId: Int, isVideo: Boolean) {
        if (isVideo) {
            feedViewModel.feedVideoItems.observe(this) { videos ->
                if (videos.isNotEmpty()) {
                    val mutableVideos = videos.toMutableList()
                    val position = mutableVideos.indexOfFirst { it.postId == scrollToItemId }
                    if (position != -1) {
                        val item = mutableVideos.removeAt(position)
                        mutableVideos.add(0, item)
                    }

                    val adapter = VideoAdapter(
                        videos = mutableVideos,
                        lifecycleOwner = this,
                        currentUser = userViewModel.currentUser.value,
                        searchViewModel = searchViewModel,
                        userViewModel = userViewModel
                    )
                    binding.feedRecyclerView.adapter = adapter
                }
            }
        } else {
            feedViewModel.feedPostItems.observe(this) { posts ->
                if (posts.isNotEmpty()) {
                    val mutablePosts = posts.toMutableList()
                    val position = mutablePosts.indexOfFirst { it.postId == scrollToItemId }
                    if (position != -1) {
                        val item = mutablePosts.removeAt(position)
                        mutablePosts.add(0, item)
                    }

                    val adapter = MyPostAdapter(
                        context = this,
                        movies = mutablePosts,
                        userViewModel = userViewModel,
                        searchViewModel = searchViewModel
                    )
                    binding.feedRecyclerView.adapter = adapter
                }
            }
        }

        feedViewModel.isLoading.observe(this) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        feedViewModel.error.observe(this) { error ->
            binding.errorText.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.errorText.text = error
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
            val intent = Intent(this, InteractionsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            ActivityNavigationHelper.removeLastOpenedSettingsActivity()
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.feedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FeedActivity)
            setHasFixedSize(true)

            // Optional: Add item decoration for spacing
            addItemDecoration(
                DividerItemDecoration(
                    this@FeedActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    companion object {
        private const val SCROLL_TO_ITEM_ID = "scroll_to_item_id"
        private const val IS_VIDEO = "is_video"
        private const val ITEM_IDS = "item_ids"
        private const val INTERACTION_TYPE = "interaction_type"

        fun newIntent(
            context: Context,
            items: List<PostDto>,
            scrollToItem: PostDto,
            isVideo: Boolean,
            interactionType: String
        ): Intent {
            val itemIds = items.map { it.postId }
            return Intent(context, FeedActivity::class.java).apply {
                putIntegerArrayListExtra(ITEM_IDS, ArrayList(itemIds))
                putExtra(SCROLL_TO_ITEM_ID, scrollToItem.postId)
                putExtra(IS_VIDEO, isVideo)
                putExtra(INTERACTION_TYPE, interactionType)
            }
        }
    }
}
