import android.R
import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.GenreEntity
import com.example.firedatabase_assis.postgres.Genres
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


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

        val languageAdapter = ArrayAdapter(context, R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
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

        val regionAdapter = ArrayAdapter(context, R.layout.simple_spinner_item, regions)
        regionAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        spinner.adapter = regionAdapter
    }
}

object GenresManager {
    private val genres = mutableSetOf<GenreEntity>()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val genresApi: Genres = retrofit.create(Genres::class.java)

    // Initialize with default genres
    suspend fun init() {
        try {
            insertDefaultGenres()
        } catch (e: Exception) {
            Log.e("DataManager", "Error loading genres", e)
        }
    }


    // Method to add a new genre
    suspend fun addGenre(genre: GenreEntity) {
        try {
            val response = genresApi.addGenre(genre)
            if (response.isSuccessful) {
                genres.add(genre)
            } else {
                Log.e("DataManager", "Failed to add genre: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("DataManager", "Error adding genre", e)
        }
    }

    // Method to insert default genres using API
    private suspend fun insertDefaultGenres() {
        try {
            val response = genresApi.insertDefaultGenres()
            if (response.isSuccessful) {
                init()
            } else {
                Log.e(
                    "DataManager",
                    "Failed to insert default genres: ${response.errorBody()?.string()}"
                )
            }
        } catch (e: Exception) {
            Log.e("DataManager", "Error inserting default genres", e)
        }
    }
}