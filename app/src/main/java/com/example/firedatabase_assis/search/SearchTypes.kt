package com.example.firedatabase_assis.search

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Movie(
    val id: Int,
    val title: String,
    val poster_path: String?,
    val overview: String,
    val mediaType: String,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
    val voteAverage: Double,
    val firstAirDate: String,
    val voteCount: Int,
    val genreIds: List<Int>
) : Parcelable

@Parcelize
data class TV(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val overview: String,
    val mediaType: String,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
    val voteAverage: Double,
    val firstAirDate: String,
    val voteCount: Int,
    val genreIds: List<Int>
) : Parcelable

@Parcelize
data class Person(
    val id: Int,
    val name: String,
    val profile_path: String?,
    val gender: Int,
    val knownForDepartment: String,
    val knownFor: List<String>,
    val mediaType: String = "person"
) : Parcelable

@Parcelize
data class PersonDetails(
    val id: Int,
    val name: String,
    val biography: String,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val profilePath: String?,
    val knownForDepartment: String?,
) : Parcelable

data class CastMember(
    val id: Int,
    val name: String,
    val character: String,
    val episodeCount: Int,
    val profilePath: String?,
)

class CastDiffCallback : DiffUtil.ItemCallback<CastMember>() {
    override fun areItemsTheSame(oldItem: CastMember, newItem: CastMember): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CastMember, newItem: CastMember): Boolean {
        return oldItem == newItem
    }
}


