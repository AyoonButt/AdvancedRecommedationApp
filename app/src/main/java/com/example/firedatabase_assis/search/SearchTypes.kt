package com.example.firedatabase_assis.search

import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class MediaItem {
    abstract val id: Int
    abstract val title: String
    abstract val poster_path: String?
    abstract val overview: String
    abstract val mediaType: String
    abstract val originalLanguage: String
    abstract val originalTitle: String
    abstract val popularity: Double
    abstract val voteAverage: Double
    abstract val firstAirDate: String
    abstract val voteCount: Int
    abstract val genreIds: List<Int>
}

data class Movie(
    override val id: Int,
    override val title: String,
    override val poster_path: String?,
    override val overview: String,
    override val mediaType: String = "movie",
    override val originalLanguage: String,
    override val originalTitle: String,
    override val popularity: Double,
    override val voteAverage: Double,
    override val firstAirDate: String,
    override val voteCount: Int,
    override val genreIds: List<Int>
) : MediaItem()


sealed class ViewPagerItem {
    data class Image(val path: String) : ViewPagerItem()
    data class Video(val key: String) : ViewPagerItem()

}

data class TV(
    override val id: Int,
    override val title: String,
    override val poster_path: String?,
    override val overview: String,
    override val mediaType: String = "tv",
    override val originalLanguage: String,
    override val originalTitle: String,
    override val popularity: Double,
    override val voteAverage: Double,
    override val firstAirDate: String,
    override val voteCount: Int,
    override val genreIds: List<Int>
) : MediaItem()

fun Movie.toMediaItem(): MediaItem {
    return this
}

fun TV.toMediaItem(): MediaItem {
    return this
}


data class Person(
    val id: Int,
    val name: String,
    val profile_path: String?,
    val gender: Int,
    val known_for_department: String,
    val media_type: String = "person"
)


data class PersonDetails(
    val id: Int,
    val name: String,
    val biography: String,
    val birthday: String?,
    val deathday: String?,
    val place_of_birth: String?,
    val profile_path: String?,
    val known_for_department: String?,
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val episodeCount: Int,
    val profilePath: String?,
)


class FlowDebouncer<T>(private val timeoutMs: Long) {
    private val _flow = MutableSharedFlow<T>()
    val flow = _flow.debounce(timeoutMs)
        .stateIn(
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun emit(value: T) {
        CoroutineScope(Dispatchers.Main).launch {
            _flow.emit(value)
        }
    }
}


data class SearchResponse(
    val mediaItems: List<MediaItem>,
    val profiles: List<Person>,
    val currentPage: Int,
    val totalPages: Int
)


class CastDiffCallback : DiffUtil.ItemCallback<CastMember>() {
    override fun areItemsTheSame(oldItem: CastMember, newItem: CastMember): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CastMember, newItem: CastMember): Boolean {
        return oldItem == newItem
    }
}


