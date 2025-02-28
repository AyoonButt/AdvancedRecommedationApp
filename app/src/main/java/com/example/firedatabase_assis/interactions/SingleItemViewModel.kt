package com.example.firedatabase_assis.interactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.PostInteractions
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.UserPostInteractionDto
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SingleItemViewModel : ViewModel() {

    private val _state = MutableLiveData<SingleItemState>()
    val state: LiveData<SingleItemState> = _state

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val postsService = retrofit.create(Posts::class.java)
    private val postInteractionsService = retrofit.create(PostInteractions::class.java)

    fun updateInteraction(
        userId: Int,
        postId: Int,
        likeState: Boolean,
        saveState: Boolean,
        commentButtonPressed: Boolean,
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            try {
                val interactionData = UserPostInteractionDto(
                    interactionId = 0,
                    userId = userId,
                    postId = postId,
                    startTimestamp = startTime.toString(),
                    endTimestamp = endTime.toString(),
                    likeState = likeState,
                    saveState = saveState,
                    commentButtonPressed = commentButtonPressed
                )
                postInteractionsService.saveInteractionData(interactionData)
            } catch (e: Exception) {
                // Handle error
                println("Failed to save interaction: ${e.message}")
            }
        }
    }

    fun updateLikeCount(postId: Int) {
        viewModelScope.launch {
            try {
                postsService.updateLikeCount(postId)
            } catch (e: Exception) {
                // Handle error
                println("Failed to update like count: ${e.message}")
            }
        }
    }


}

