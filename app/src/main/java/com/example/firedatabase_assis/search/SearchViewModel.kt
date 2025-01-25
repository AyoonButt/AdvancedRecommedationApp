package com.example.firedatabase_assis.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firedatabase_assis.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Collections

class SearchViewModel : ViewModel() {

    private val client = OkHttpClient()
    private val gson = Gson()

    private val _profiles = MutableLiveData<List<Person>>()
    val profiles: LiveData<List<Person>> = _profiles

    private val _mediaItems = MutableLiveData<List<MediaItem>>()
    val mediaItems: LiveData<List<MediaItem>> = _mediaItems

    private val mediaItemList = mutableListOf<MediaItem>()

    private val _isLoadingPage = MutableStateFlow(false)
    private val _isSearching = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = combine(
        _isLoadingPage,
        _isSearching
    ) { loadingPage, searching ->
        loadingPage || searching
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _selectedItem = MutableLiveData<Any>()
    val selectedItem: LiveData<Any> = _selectedItem

    private val searchDebouncer = FlowDebouncer<String>(300L)

    private var currentQuery = ""
    private var currentPage = 1
    private var totalPages = 1
    private var searchJob: Job? = null

    private val moviesList = Collections.synchronizedList(mutableListOf<Movie>())
    private val tvList = Collections.synchronizedList(mutableListOf<TV>())
    private val profilesList = Collections.synchronizedList(mutableListOf<Person>())

    sealed class PagingState {
        object Idle : PagingState()
        object Loading : PagingState()
        data class Error(val message: String) : PagingState()
    }

    private val _pagingState = MutableStateFlow<PagingState>(PagingState.Idle)
    val pagingState: StateFlow<PagingState> = _pagingState.asStateFlow()

    private var isLoadingNextPage = false


    init {
        viewModelScope.launch {
            searchDebouncer.flow
                .filterNotNull()
                .filterNot { it.isBlank() }
                .distinctUntilChanged()
                .onEach { query ->
                    searchJob?.cancel()
                    clearLists()
                    _isSearching.value = true
                    if (query != null) {
                        currentQuery = query
                    }
                    currentPage = 1
                    totalPages = 1
                    searchJob = launch {
                        if (query != null) {
                            performSearch(query)
                        }
                    }
                }
                .catch { e ->
                    e.printStackTrace()
                }
                .collect()
        }
    }

    private suspend fun performSearch(query: String) {
        try {
            _isSearching.value = true
            val response = withContext(Dispatchers.IO) {
                fetchSearchResults(query, currentPage)
            }

            // Handle profiles first
            synchronized(profilesList) {
                profilesList.clear()
                profilesList.addAll(response.profiles)
                _profiles.value = profilesList.toList()  // Update profiles immediately
            }

            // Then handle media items
            synchronized(moviesList) { moviesList.clear() }
            synchronized(tvList) { tvList.clear() }

            response.mediaItems.forEach {
                when (it) {
                    is Movie -> synchronized(moviesList) { moviesList.add(it) }
                    is TV -> synchronized(tvList) { tvList.add(it) }
                }
            }

            updateLiveData()
            currentPage = response.currentPage
            totalPages = response.totalPages
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isSearching.value = false
        }
    }

    private fun fetchSearchResults(query: String, page: Int): SearchResponse {
        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/search/multi?query=$query&include_adult=false&language=en-US&page=$page")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected response ${response.code}")

            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            val searchResult = gson.fromJson(responseBody, JsonObject::class.java)

            val results = searchResult.getAsJsonArray("results")
            val totalPages = searchResult.get("total_pages")?.asInt ?: 1
            val currentPage = searchResult.get("page")?.asInt ?: page

            val movies = mutableListOf<Movie>()
            val tvShows = mutableListOf<TV>()
            val profiles = mutableListOf<Person>()

            results.forEach { resultJson ->
                val result = gson.fromJson(resultJson, JsonObject::class.java)
                when (result.get("media_type")?.asString) {
                    "movie" -> {
                        val movie = gson.fromJson(result, Movie::class.java)
                        if (!movie.poster_path.isNullOrEmpty()) movies.add(movie)
                    }

                    "tv" -> {
                        val tv = gson.fromJson(result, TV::class.java)
                        if (!tv.poster_path.isNullOrEmpty()) tvShows.add(tv)
                    }

                    "person" -> {
                        val person = gson.fromJson(result, Person::class.java)
                        if (!person.profile_path.isNullOrEmpty()) profiles.add(person)
                    }
                }
            }

            SearchResponse(
                mediaItems = movies + tvShows,
                profiles = profiles,
                currentPage = currentPage,
                totalPages = totalPages
            )
        }
    }


    fun loadNextPage() {
        if (isLoadingNextPage || currentPage >= totalPages || currentQuery.isBlank()) return

        isLoadingNextPage = true
        _pagingState.value = PagingState.Loading

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    fetchSearchResults(currentQuery, currentPage + 1)
                }

                synchronized(mediaItemList) {
                    response.mediaItems.forEach { newItem ->
                        when (newItem) {
                            is Movie -> if (!moviesList.any { it.id == newItem.id }) {
                                moviesList.add(newItem)
                            }

                            is TV -> if (!tvList.any { it.id == newItem.id }) {
                                tvList.add(newItem)
                            }
                        }
                    }
                }

                synchronized(profilesList) {
                    response.profiles.forEach { newProfile ->
                        if (!profilesList.any { it.id == newProfile.id }) {
                            profilesList.add(newProfile)
                        }
                    }
                }

                currentPage = response.currentPage
                totalPages = response.totalPages
                updateLiveData()
                _pagingState.value = PagingState.Idle
            } catch (e: Exception) {
                _pagingState.value = PagingState.Error(e.message ?: "Unknown error")
            } finally {
                isLoadingNextPage = false
            }
        }
    }

    private fun clearLists() {
        synchronized(moviesList) { moviesList.clear() }
        synchronized(tvList) { tvList.clear() }
        synchronized(profilesList) { profilesList.clear() }
        _profiles.value = emptyList()
        _mediaItems.value = emptyList()
    }

    private fun updateLiveData() {
        viewModelScope.launch(Dispatchers.Main) {
            synchronized(mediaItemList) {
                mediaItemList.clear()
                // Combine and sort all media items by popularity
                val sortedItems = (moviesList + tvList).sortedByDescending { it.popularity }
                mediaItemList.addAll(sortedItems)
            }
            _profiles.value = synchronized(profilesList) { profilesList.toList() }
            _mediaItems.value = synchronized(mediaItemList) { mediaItemList.toList() }
        }
    }

    fun onSearchInput(query: String) {
        viewModelScope.launch {
            searchDebouncer.emit(query)
        }
    }

    sealed class NavigationState {
        data class ShowPoster(val id: Int, val isMovie: Boolean) : NavigationState()
        data class ShowPerson(val id: Int) : NavigationState()
        object Back : NavigationState()
        object Close : NavigationState()
    }


    private val _navigationEvent = MutableLiveData<NavigationState>()
    val navigationEvent: LiveData<NavigationState> = _navigationEvent

    fun navigate(state: NavigationState) {
        _navigationEvent.value = state
    }


    fun setSelectedItem(item: Any) {
        viewModelScope.launch(Dispatchers.Main) {
            println("Setting selected item: $item")
            _selectedItem.value = item
        }
    }


    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
