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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Collections

class SearchViewModel : ViewModel() {
    private lateinit var navigationManager: NavigationManager
    private val client = OkHttpClient()
    private val gson = Gson()

    private val _profiles = MutableLiveData<List<Person>>()
    val profiles: LiveData<List<Person>> = _profiles

    private val _moviesAndShows = MutableLiveData<Pair<List<Movie>, List<TV>>>()
    val moviesAndShows: LiveData<Pair<List<Movie>, List<TV>>> = _moviesAndShows

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

    private var currentQuery = ""
    private var currentPage = 1
    private var totalPages = 1
    private var searchJob: Job? = null

    private val moviesList = Collections.synchronizedList(mutableListOf<Movie>())
    private val tvList = Collections.synchronizedList(mutableListOf<TV>())
    private val profilesList = Collections.synchronizedList(mutableListOf<Person>())

    fun search(query: String) {
        if (query == currentQuery) return
        if (_isSearching.value) return

        searchJob?.cancel()
        viewModelScope.launch {
            try {
                _isSearching.emit(true)
                currentQuery = query
                currentPage = 1
                totalPages = 1
                clearLists()
                fetchPage(1)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.emit(false)
            }
        }
    }


    fun loadNextPage() {
        if (_isLoadingPage.value || currentPage >= totalPages) return

        viewModelScope.launch {
            try {
                _isLoadingPage.emit(true)
                fetchPage(currentPage + 1)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingPage.emit(false)
            }
        }
    }

    private suspend fun fetchPage(page: Int) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.themoviedb.org/3/search/multi?query=$currentQuery&include_adult=false&language=en-US&page=$page")
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use

                    response.body?.let { responseBody ->
                        val json = responseBody.string()
                        val searchResult = gson.fromJson(json, JsonObject::class.java)
                        val results = searchResult.getAsJsonArray("results")

                        totalPages = searchResult.get("total_pages")?.asInt ?: 1
                        currentPage = page

                        synchronized(moviesList) {
                            synchronized(tvList) {
                                synchronized(profilesList) {
                                    results?.forEach { resultJson ->
                                        val result =
                                            gson.fromJson(resultJson, JsonObject::class.java)
                                        when (result.get("media_type")?.asString) {
                                            "movie" -> {
                                                val movie = gson.fromJson(result, Movie::class.java)
                                                if (!movie.poster_path.isNullOrEmpty()) {
                                                    moviesList.add(movie)
                                                }
                                            }

                                            "tv" -> {
                                                val tv = gson.fromJson(result, TV::class.java)
                                                if (!tv.poster_path.isNullOrEmpty()) {
                                                    tvList.add(tv)
                                                }
                                            }

                                            "person" -> {
                                                val person =
                                                    gson.fromJson(result, Person::class.java)
                                                if (!person.profile_path.isNullOrEmpty()) {
                                                    profilesList.add(person)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        updateLiveData()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun clearLists() {
        synchronized(moviesList) { moviesList.clear() }
        synchronized(tvList) { tvList.clear() }
        synchronized(profilesList) { profilesList.clear() }
        updateLiveData()
    }

    private fun updateLiveData() {
        viewModelScope.launch(Dispatchers.Main) {
            _profiles.value = synchronized(profilesList) { profilesList.toList() }
            _moviesAndShows.value = synchronized(moviesList) {
                synchronized(tvList) {
                    Pair(moviesList.toList(), tvList.toList())
                }
            }
        }
    }


    sealed class NavigationState {
        data class ShowPoster(val item: Any) : NavigationState()
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
            _selectedItem.value = item
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}