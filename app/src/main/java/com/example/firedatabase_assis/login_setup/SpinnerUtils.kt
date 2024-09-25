package com.example.firedatabase_assis.utils

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.firedatabase_assis.database.Genres
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


object SpinnerUtils {
    fun setupLanguageSpinner(context: Context, spinner: Spinner) {
        val languages = mutableListOf("Select Language")
        languages.addAll(
            listOf(
                "Abkhazian", "Afar", "Afrikaans", "Akan", "Albanian", "Amharic", "Arabic",
                "Aragonese", "Armenian", "Assamese", "Avaric", "Avestan", "Aymara",
                "Azerbaijani", "Bambara", "Bashkir", "Basque", "Belarusian", "Bengali",
                "Bihari", "Bislama", "Bosnian", "Breton", "Bulgarian", "Burmese", "Catalan",
                "Chamorro", "Chechen", "Chichewa; Nyanja", "Chinese", "Corsican", "Cree",
                "Croatian", "Czech", "Danish", "Divehi", "Dutch", "Dzongkha", "English",
                "Esperanto", "Estonian", "Ewe", "Faroese", "Fijian", "Finnish", "French",
                "Frisian", "Gaelic", "Galician", "Ganda", "Georgian", "German", "Greek",
                "Guarani", "Gujarati", "Haitian; Haitian Creole", "Hausa", "Hebrew", "Herero",
                "Hindi", "Hiri Motu", "Hungarian", "Icelandic", "Ido", "Igbo", "Indonesian",
                "Interlingua", "Interlingue", "Inuktitut", "Inupiaq", "Irish", "Italian",
                "Japanese", "Javanese", "Kannada", "Kanuri", "Kashmiri", "Kazakh", "Khmer",
                "Kikuyu", "Kinyarwanda", "Kirghiz", "Kongo", "Korean", "Kurdish", "Kuanyama",
                "Latin", "Latvian", "Letzeburgesch", "Limburgish", "Lingala", "Lithuanian",
                "Luba-Katanga", "Macedonian", "Malagasy", "Malay", "Malayalam", "Maltese",
                "Manx", "Maori", "Marathi", "Marshall", "Moldavian", "Mongolian", "Nauru",
                "Navajo", "Ndebele", "Nepali", "Northern Sami", "Norwegian", "Norwegian Bokmål",
                "Norwegian Nynorsk", "Nyanja", "Occitan", "Ojibwa", "Oriya", "Oromo",
                "Ossetian; Ossetic", "Pali", "Punjabi", "Persian", "Polish", "Portuguese",
                "Pushto", "Quechua", "Raeto-Romance", "Romanian", "Romansh", "Rundi", "Russian",
                "Sami", "Samoan", "Sango", "Sanskrit", "Serbian", "Serbo-Croatian", "Sesotho",
                "Setswana", "Shona", "Sindhi", "Sinhalese", "Slovak", "Slovenian", "Somali",
                "Spanish", "Sundanese", "Swahili", "Swati", "Swedish", "Tagalog", "Tahitian",
                "Tajik", "Tamil", "Tatar", "Telugu", "Thai", "Tibetan", "Tigrinya", "Tonga",
                "Tsonga", "Tswana", "Turkish", "Turkmen", "Twi", "Uighur", "Ukrainian", "Urdu",
                "Uzbek", "Venda", "Vietnamese", "Volapük", "Walloon", "Welsh", "Wolof",
                "Western Frisian", "Xhosa", "Yiddish", "Yoruba", "Zhuang", "Zulu"
            )
        )

        val languageAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = languageAdapter
    }

    fun setupRegionSpinner(context: Context, spinner: Spinner) {
        val regions = mutableListOf("Select Region")
        regions.addAll(
            listOf(
                "Andorra", "United Arab Emirates", "Antigua and Barbuda", "Albania", "Angola",
                "Argentina", "Austria", "Australia", "Azerbaijan", "Bosnia and Herzegovina",
                "Barbados", "Belgium", "Burkina Faso", "Bulgaria", "Bahrain", "Bermuda",
                "Bolivia", "Brazil", "Bahamas", "Belarus", "Belize", "Canada",
                "Democratic Republic of the Congo (Kinshasa)", "Switzerland", "Côte d’Ivoire",
                "Chile", "Cameroon", "Colombia", "Costa Rica", "Cuba", "Cape Verde", "Cyprus",
                "Czech Republic", "Germany", "Denmark", "Dominican Republic", "Algeria",
                "Ecuador", "Estonia", "Egypt", "Spain", "Finland", "Fiji", "France",
                "United Kingdom", "French Guiana", "Ghana", "Gibraltar", "Guadeloupe",
                "Equatorial Guinea", "Greece", "Guatemala", "Guyana", "Hong Kong SAR China",
                "Honduras", "Croatia", "Hungary", "Indonesia", "Ireland", "Israel", "India",
                "Iraq", "Iceland", "Italy", "Jamaica", "Jordan", "Japan", "Kenya", "South Korea",
                "Kuwait", "Lebanon", "St. Lucia", "Liechtenstein", "Lithuania", "Luxembourg",
                "Latvia", "Libya", "Morocco", "Monaco", "Moldova", "Montenegro", "Madagascar",
                "Macedonia", "Mali", "Malta", "Mauritius", "Malawi", "Mexico", "Malaysia",
                "Mozambique", "Niger", "Nigeria", "Nicaragua", "Netherlands", "Norway",
                "New Zealand", "Oman", "Panama", "Peru", "French Polynesia", "Papua New Guinea",
                "Philippines", "Pakistan", "Poland", "Palestinian Territories", "Portugal",
                "Paraguay", "Qatar", "Romania", "Serbia", "Russia", "Saudi Arabia", "Seychelles",
                "Sweden", "Singapore", "Slovenia", "Slovakia", "San Marino", "Senegal",
                "El Salvador", "Turks & Caicos Islands", "Chad", "Thailand", "Tunisia", "Turkey",
                "Trinidad & Tobago", "Taiwan", "Tanzania", "Ukraine", "Uganda", "United States",
                "Uruguay", "Vatican City", "Venezuela", "Kosovo", "Yemen", "South Africa",
                "Zambia", "Zimbabwe"
            )
        )

        val regionAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, regions)
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = regionAdapter
    }
}

data class Genre(val id: Int, var name: String)

object DataManager {
    val genres = mutableSetOf<Genre>()

    // Initialize with default genres
    init {
        transaction {
            // Check if the database has any genres
            if (Genres.selectAll().empty()) {
                // If no genres in the database, add the default genres
                val defaultGenres = listOf(
                    Genre(28, "Action"),
                    Genre(12, "Adventure"),
                    Genre(16, "Animation"),
                    Genre(35, "Comedy"),
                    Genre(80, "Crime"),
                    Genre(99, "Documentary"),
                    Genre(18, "Drama"),
                    Genre(10751, "Family"),
                    Genre(14, "Fantasy"),
                    Genre(36, "History"),
                    Genre(27, "Horror"),
                    Genre(10402, "Music"),
                    Genre(9648, "Mystery"),
                    Genre(10749, "Romance"),
                    Genre(878, "Science Fiction"),
                    Genre(10770, "TV Movie"),
                    Genre(53, "Thriller"),
                    Genre(10752, "War"),
                    Genre(37, "Western"),
                    Genre(10759, "Action & Adventure"),
                    Genre(10762, "Kids"),
                    Genre(10763, "News"),
                    Genre(10764, "Reality"),
                    Genre(10765, "Sci-Fi & Fantasy"),
                    Genre(10766, "Soap"),
                    Genre(10767, "Talk"),
                    Genre(10768, "War & Politics")
                )

                // Insert default genres into the database
                defaultGenres.forEach { genre ->
                    Genres.insert {
                        it[genreId] = genre.id
                        it[genreName] = genre.name
                    }
                }

                // Add to the in-memory set
                genres.addAll(defaultGenres)
            } else {
                // Load genres from the database
                Genres.selectAll().forEach {
                    val genre = Genre(
                        id = it[Genres.genreId],
                        name = it[Genres.genreName]
                    )
                    genres.add(genre)
                }
            }
        }
    }

    fun getGenres(): List<Genre> {
        return genres.toList()
    }

    fun getGenreIds(names: List<String>): List<Int> {
        return genres.filter { it.name in names }.map { it.id }
    }

    // Optionally: Method to add new genres to the database and the in-memory set
    fun addGenre(genre: Genre) {
        transaction {
            Genres.insert {
                it[genreId] = genre.id
                it[genreName] = genre.name
            }
            genres.add(genre)
        }
    }
}


