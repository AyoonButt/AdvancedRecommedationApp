package com.example.firedatabase_assis.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

class PersonDetailFragment : Fragment() {
    private lateinit var viewModel: SearchViewModel
    private lateinit var userViewModel: UserViewModel
    private val client = OkHttpClient()
    private lateinit var creditsAdapter: MediaItemAdapter
    private var startTimestamp: String? = null

    private var isBackNavigation = false

    companion object {
        fun newInstance(personId: Int): PersonDetailFragment {
            return PersonDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("person_id", personId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        startTimestamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val view = inflater.inflate(R.layout.fragment_person_details, container, false)
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        userViewModel = UserViewModel.getInstance(requireActivity().application)
        setupViews(view)
        loadPersonDetails()
        return view
    }

    private fun setupViews(view: View) {
        // Setup back and close buttons
        view.findViewById<ImageView>(R.id.back).setOnClickListener {
            isBackNavigation = true
            viewModel.navigate(SearchViewModel.NavigationState.Back)
        }

        view.findViewById<ImageView>(R.id.close).setOnClickListener {
            viewModel.navigate(SearchViewModel.NavigationState.Close)
        }

        // Initialize the adapter
        creditsAdapter = MediaItemAdapter(
            onItemClick = { item ->
                val isMovie = item is Movie
                viewModel.setSelectedItem(item)

                viewModel.navigate(
                    SearchViewModel.NavigationState.ShowPoster(
                        item.id,
                        isMovie = isMovie
                    )
                )
            },
            isRecommendation = false
        )

        // Setup RecyclerView for credits
        view.findViewById<RecyclerView>(R.id.creditsRecyclerView).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = creditsAdapter
        }
    }

    private fun loadPersonDetails() {
        val personId = arguments?.getInt("person_id") ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Person details request
                val detailsUrl = "https://api.themoviedb.org/3/person/$personId"
                val detailsRequest = Request.Builder()
                    .url(detailsUrl)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(detailsRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = response.body?.string()
                        val person = Gson().fromJson(jsonData, PersonDetails::class.java)
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            updatePersonUI(person)
                        }
                    }
                }

                // Credits request
                val creditsUrl = "https://api.themoviedb.org/3/person/$personId/combined_credits"
                val creditsRequest = Request.Builder()
                    .url(creditsUrl)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(creditsRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = response.body?.string()
                        val mediaItems = parseCredits(jsonData)
                            .distinctBy { it.id } // Remove duplicates based on id
                            .sortedByDescending { it.popularity }
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            creditsAdapter.submitList(mediaItems)

                            // Hide or show the credits section based on whether there are items
                            view?.apply {
                                findViewById<TextView>(R.id.creditsLabel).visibility =
                                    if (mediaItems.isNotEmpty()) View.VISIBLE else View.GONE
                                findViewById<RecyclerView>(R.id.creditsRecyclerView).visibility =
                                    if (mediaItems.isNotEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updatePersonUI(person: PersonDetails) {
        view?.apply {
            // Name is always displayed
            findViewById<TextView>(R.id.name).text = person.name

            // Place of birth - hide if empty
            val birthplace = person.place_of_birth
            findViewById<TextView>(R.id.placeOfBirth).text = birthplace ?: ""
            findViewById<LinearLayout>(R.id.birthplaceContainer).visibility =
                if (!birthplace.isNullOrEmpty()) View.VISIBLE else View.GONE

            // Department - hide if empty
            val department = person.known_for_department
            findViewById<TextView>(R.id.knownFor).text = department ?: ""
            findViewById<LinearLayout>(R.id.knownForContainer).visibility =
                if (!department.isNullOrEmpty()) View.VISIBLE else View.GONE

            // Age or age range - hide if no birth date
            val birthDate = person.birthday
            if (!birthDate.isNullOrEmpty()) {
                val ageOrRange = person.deathday?.let { death -> "$birthDate - $death" }
                    ?: "Age: ${calculateAge(birthDate)}"
                findViewById<TextView>(R.id.age_or_range).text = ageOrRange
                findViewById<LinearLayout>(R.id.ageContainer).visibility = View.VISIBLE
            } else {
                findViewById<LinearLayout>(R.id.ageContainer).visibility = View.GONE
            }

            // Biography - hide entire card if empty
            val biography = person.biography
            findViewById<TextView>(R.id.biography).text = biography
            findViewById<androidx.cardview.widget.CardView>(R.id.biographyCard).visibility =
                if (!biography.isNullOrEmpty()) View.VISIBLE else View.GONE

            // Profile image
            person.profile_path?.let { path ->
                Glide.with(this)
                    .load("https://image.tmdb.org/t/p/original$path")
                    .into(findViewById(R.id.personImage))
            }
        }
    }

    private fun parseCredits(jsonData: String?): List<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()

        jsonData?.let {
            val jsonObject = JSONObject(it)
            val cast = jsonObject.getJSONArray("cast")

            for (i in 0 until cast.length()) {
                val item = cast.getJSONObject(i)
                when (item.getString("media_type")) {
                    "movie" -> mediaItems.add(
                        Gson().fromJson(item.toString(), Movie::class.java).toMediaItem()
                    )

                    "tv" -> mediaItems.add(
                        Gson().fromJson(item.toString(), TV::class.java).toMediaItem()
                    )
                }
            }
        }

        return mediaItems
    }

    private fun calculateAge(birthDate: String): Int {
        // Simplified age calculation
        val birthYear = birthDate.take(4).toIntOrNull() ?: return 0
        return java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - birthYear
    }

    private fun saveCurrentViewDuration() {
        arguments?.let { args ->
            val id = args.getInt("person_id")
            startTimestamp?.let { startTime ->
                val endTimestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                userViewModel.currentUser.value?.userId?.let { userId ->
                    viewModel.saveViewDuration(
                        tmdbId = id,
                        type = "person",
                        startTime = startTime,
                        endTime = endTimestamp,
                        userId = userId,
                        isBackNavigation = isBackNavigation
                    )
                }
                startTimestamp = null
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isBackNavigation) {
            saveCurrentViewDuration()
        }
    }

    override fun onDetach() {
        if (isBackNavigation) {
            saveCurrentViewDuration()
        }
        super.onDetach()
    }
}