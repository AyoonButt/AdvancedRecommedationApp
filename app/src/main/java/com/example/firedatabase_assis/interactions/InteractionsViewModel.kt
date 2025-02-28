package com.example.firedatabase_assis.interactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Comments
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.PostInteractions
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.TrailerInteractions
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class InteractionsViewModel(private val userViewModel: UserViewModel) : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val postInteractionApi: PostInteractions = retrofit.create(PostInteractions::class.java)
    private val trailerInteractionApi: TrailerInteractions =
        retrofit.create(TrailerInteractions::class.java)
    private val postsApi: Posts = retrofit.create(Posts::class.java)
    private val commentsApi: Comments = retrofit.create(Comments::class.java)


    private val _state = MutableLiveData(InteractionsState())
    val state: LiveData<InteractionsState> = _state

    private val _uiEvent = MutableLiveData<Event<InteractionUiEvent>>()
    val uiEvent: LiveData<Event<InteractionUiEvent>> = _uiEvent

    private var currentPage = 0
    private val pageSize = 20

    init {
        userViewModel.currentUser.observeForever { user ->
            if (user == null) {
                _state.value = _state.value?.copy(
                    error = "Please login to view your interactions",
                    isLoading = false,
                    items = emptyList()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userViewModel.currentUser.removeObserver { }
    }

    fun onEvent(event: InteractionEvent) {
        when (event) {
            is InteractionEvent.SelectInteractionType -> {
                currentPage = 0  // Reset page when changing type
                _state.value = _state.value?.copy(
                    selectedInteraction = event.type,
                    items = emptyList(),
                    isLoading = _state.value?.contentType != null  // Only set loading if we have content type
                )
                // Only load if we have both selections AND a logged-in user
                if (_state.value?.contentType != null && userViewModel.currentUser.value != null) {
                    loadInteractions()
                }
            }

            is InteractionEvent.SelectContentType -> {
                currentPage = 0  // Reset page when changing type
                _state.value = _state.value?.copy(
                    contentType = event.type,
                    items = emptyList(),
                    isLoading = _state.value?.selectedInteraction != null  // Only set loading if we have interaction type
                )
                // Only load if we have both selections AND a logged-in user
                if (_state.value?.selectedInteraction != null && userViewModel.currentUser.value != null) {
                    loadInteractions()
                }
            }

            is InteractionEvent.SelectItem -> {
                val posts = state.value?.items ?: emptyList()
                val selectedPost = posts.find { it.postId == event.itemId }
                selectedPost?.let {
                    _uiEvent.value = Event(
                        InteractionUiEvent.NavigateToFeed(item = it)
                    )
                }
            }

            is InteractionEvent.NavigateToSingleItem -> {
                _uiEvent.value = Event(
                    InteractionUiEvent.NavigateToSingleItem(
                        item = event.item
                    )
                )
            }

            InteractionEvent.NavigateBack -> {
                _uiEvent.value = Event(InteractionUiEvent.NavigateBack)
            }

            else -> {
                // Handle any other cases or add remaining event types
            }
        }
    }

    fun getCurrentInteractionType(): InteractionType {
        return state.value?.selectedInteraction ?: InteractionType.LIKED
    }

    private fun loadInteractions() {
        if (userViewModel.currentUser.value == null) {
            _state.value = _state.value?.copy(
                error = "Please login to view your interactions",
                isLoading = false,
                items = emptyList()
            )
            return
        }

        viewModelScope.launch {
            try {
                val userId = userViewModel.currentUser.value?.userId
                val state = _state.value ?: return@launch

                when {
                    // Handle Comments
                    state.selectedInteraction == InteractionType.COMMENTED -> {
                        val commentType = when (state.contentType) {
                            ContentType.POSTS -> "post"
                            ContentType.VIDEOS -> "trailer"
                        }

                        val commentsResponse = commentsApi.getCommentsByUserIdAndType(
                            userId = userId!!,
                            commentType = commentType,
                            page = currentPage,
                            pageSize = pageSize
                        )

                        // Get comments or empty list if response failed
                        val comments = if (commentsResponse.isSuccessful) {
                            commentsResponse.body() ?: emptyList()
                        } else {
                            emptyList()
                        }

                        // Get unique IDs from comments
                        val contentIds = comments.map { it.postId }.distinct()

                        if (contentIds.isEmpty()) {
                            _state.value = state.copy(
                                objects = emptyList(),
                                items = emptyList(),
                                isLoading = false,
                                error = null
                            )
                            return@launch
                        }

                        // Fetch content based on type
                        val contentResponse = when (state.contentType) {
                            ContentType.POSTS -> postsApi.getPagedPostDtos(
                                interactionIds = contentIds,
                                page = 0,
                                pageSize = contentIds.size
                            )

                            ContentType.VIDEOS -> postsApi.getPagedPostDtos(
                                interactionIds = contentIds,
                                page = 0,
                                pageSize = contentIds.size
                            )
                        }

                        if (contentResponse.isSuccessful) {
                            val content = contentResponse.body() ?: emptyList()

                            // Create content with comments objects
                            val contentWithComments = content.map { post ->
                                val itemComments = comments.filter { it.postId == post.postId }
                                PostWithComments(
                                    post = post,
                                    comments = itemComments
                                )
                            }

                            _state.value = state.copy(
                                objects = if (currentPage == 0) contentWithComments else state.objects + contentWithComments,
                                items = emptyList(), // Clear items if using objects
                                isLoading = false,
                                error = null
                            )
                            currentPage++
                        } else {
                            _state.value = state.copy(
                                error = "Failed to load ${state.contentType.name.lowercase()}: ${contentResponse.message()}",
                                isLoading = false
                            )
                        }
                    }

                    // Handle Likes and Saves
                    else -> {
                        val interactionResponse = when (state.contentType) {
                            ContentType.POSTS -> when (state.selectedInteraction) {
                                InteractionType.LIKED -> postInteractionApi.getLikedPosts(userId!!)
                                InteractionType.SAVED -> postInteractionApi.getSavedPosts(userId!!)
                                else -> throw IllegalStateException("Invalid interaction type")
                            }

                            ContentType.VIDEOS -> when (state.selectedInteraction) {
                                InteractionType.LIKED -> trailerInteractionApi.getLikedTrailers(
                                    userId!!
                                )

                                InteractionType.SAVED -> trailerInteractionApi.getSavedTrailers(
                                    userId!!
                                )

                                else -> throw IllegalStateException("Invalid interaction type")
                            }
                        }

                        if (!interactionResponse.isSuccessful) {
                            _state.value = state.copy(
                                error = "Failed to load ${state.contentType.name.lowercase()}: ${interactionResponse.message()}",
                                isLoading = false
                            )
                            return@launch
                        }

                        val ids = interactionResponse.body() ?: emptyList()
                        if (ids.isEmpty()) {
                            _state.value = state.copy(
                                items = emptyList(),
                                isLoading = false,
                                error = null
                            )
                            return@launch
                        }

                        val offset = currentPage * pageSize
                        if (offset >= ids.size) return@launch

                        val pageIds = ids.subList(
                            offset,
                            minOf(offset + pageSize, ids.size)
                        )

                        if (pageIds.isEmpty()) return@launch

                        val contentResponse = postsApi.getPagedPostDtos(
                            interactionIds = pageIds,
                            page = 0,
                            pageSize = pageIds.size
                        )

                        if (contentResponse.isSuccessful) {
                            val newItems = contentResponse.body() ?: emptyList()
                            _state.value = state.copy(
                                items = if (currentPage == 0) newItems else state.items + newItems,
                                isLoading = false,
                                error = null
                            )
                            currentPage++
                        } else {
                            _state.value = state.copy(
                                error = "Failed to load ${state.contentType.name.lowercase()}: ${contentResponse.message()}",
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value?.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _state.value ?: return
        if (!currentState.isLoading) {
            _state.value = currentState.copy(isLoading = true)
            loadInteractions()
        }
    }

    fun refresh() {
        currentPage = 0
        _state.value = _state.value?.copy(
            items = emptyList(),
            isLoading = true,
            error = null
        )
        loadInteractions()
    }

    fun getCurrentContentType(): ContentType {
        return state.value?.contentType ?: ContentType.POSTS
    }

    fun getCurrentPosts(): List<PostDto> {
        return state.value?.items ?: emptyList()
    }
}