package com.example.firedatabase_assis.interactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FeedViewModel : ViewModel() {

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val postsApi = retrofit.create(Posts::class.java)

    private val _feedPostItems = MutableLiveData<List<PostDto>>()
    val feedPostItems: LiveData<List<PostDto>> get() = _feedPostItems

    private val _feedVideoItems = MutableLiveData<List<PostDto>>()
    val feedVideoItems: LiveData<List<PostDto>> get() = _feedVideoItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadFeedItems(itemIds: List<Int>, isVideo: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = postsApi.getPagedPostDtos(
                    interactionIds = itemIds,
                    page = 0,  // Initial page
                    pageSize = itemIds.size  // Load all items since this is for feed
                )

                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    if (isVideo) {
                        _feedVideoItems.value = posts
                    } else {
                        _feedPostItems.value = posts
                    }
                    _error.value = null
                } else {
                    _error.value = "Failed to load feed items"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


}
