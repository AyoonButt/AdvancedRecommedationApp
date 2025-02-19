package com.example.firedatabase_assis.interactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.PostDto

data class InteractionsState(
    val selectedInteraction: InteractionType = InteractionType.LIKED,
    val contentType: ContentType = ContentType.POSTS,
    val items: List<PostDto> = emptyList(),  // Changed from itemIds: List<Int>
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class InteractionEvent {
    data class SelectInteractionType(val type: InteractionType) : InteractionEvent()
    data class SelectContentType(val type: ContentType) : InteractionEvent()
    data class SelectItem(val itemId: Int) : InteractionEvent()
    object NavigateBack : InteractionEvent()
}

sealed class InteractionUiEvent {
    data class NavigateToFeed(val item: PostDto, val isComment: Boolean) : InteractionUiEvent()
    object NavigateBack : InteractionUiEvent()
}


// InteractionType.kt
enum class InteractionType {
    LIKED,
    SAVED,
    COMMENTED
}

// ContentType.kt
enum class ContentType {
    POSTS,
    VIDEOS
}

class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

data class SingleItemState(
    val isLoading: Boolean = false,
    val item: PostDto?,
    val error: String? = null
)

class InteractionsViewModelFactory(private val userViewModel: UserViewModel) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InteractionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InteractionsViewModel(userViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


