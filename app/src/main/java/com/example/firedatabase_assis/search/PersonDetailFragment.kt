package com.example.firedatabase_assis.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PersonDetailFragment : Fragment() {
    private lateinit var viewModel: SearchViewModel
    private val client = OkHttpClient()
    private lateinit var creditsAdapter: MovieTVAdapter

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
        val view = inflater.inflate(R.layout.fragment_person_details, container, false)
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        setupViews(view)
        loadPersonDetails()
        return view
    }

    private fun setupViews(view: View) {
        view.findViewById<ImageView>(R.id.back).setOnClickListener {
            viewModel.navigate(SearchViewModel.NavigationState.Back)
        }

        view.findViewById<ImageView>(R.id.close).setOnClickListener {
            viewModel.navigate(SearchViewModel.NavigationState.Close)
        }

        creditsAdapter = MovieTVAdapter { item ->
            viewModel.setSelectedItem(item)
            viewModel.navigate(SearchViewModel.NavigationState.ShowPoster(item))
        }

        view.findViewById<RecyclerView>(R.id.creditsRecyclerView).apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = creditsAdapter
        }
    }


    private fun loadPersonDetails() {
        val personId = arguments?.getInt("person_id") ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch person details
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

                // Fetch credits
                val creditsUrl = "https://api.themoviedb.org/3/person/$personId/combined_credits"
                val creditsRequest = Request.Builder()
                    .url(creditsUrl)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(creditsRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = response.body?.string()
                        val credits = parseCredits(jsonData)

                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            creditsAdapter.submitData(credits.first, credits.second)
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
            findViewById<TextView>(R.id.name).text = person.name
            findViewById<TextView>(R.id.placeOfBirth).text = person.placeOfBirth ?: "N/A"
            findViewById<TextView>(R.id.knownFor).text = person.knownForDepartment ?: "N/A"

            // Calculate age or birth-death range
            val ageOrRange = person.birthday?.let { birth ->
                person.deathday?.let { death -> "$birth - $death" }
                    ?: "Age: ${calculateAge(birth)}"
            } ?: "N/A"
            findViewById<TextView>(R.id.age_or_range).text = ageOrRange

            findViewById<TextView>(R.id.biography).text = person.biography

            person.profilePath?.let { path ->
                Glide.with(this)
                    .load("https://image.tmdb.org/t/p/original$path")
                    .into(findViewById(R.id.personImage))
            }
        }
    }

    private fun parseCredits(jsonData: String?): Pair<List<Movie>, List<TV>> {
        val movies = mutableListOf<Movie>()
        val tvShows = mutableListOf<TV>()

        jsonData?.let {
            val jsonObject = JSONObject(it)
            val cast = jsonObject.getJSONArray("cast")

            for (i in 0 until cast.length()) {
                val item = cast.getJSONObject(i)
                when (item.getString("media_type")) {
                    "movie" -> movies.add(Gson().fromJson(item.toString(), Movie::class.java))
                    "tv" -> tvShows.add(Gson().fromJson(item.toString(), TV::class.java))
                }
            }
        }

        return Pair(movies, tvShows)
    }

    private fun calculateAge(birthDate: String): Int {
        // Simplified age calculation
        val birthYear = birthDate.take(4).toIntOrNull() ?: return 0
        return java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - birthYear
    }


    // Add to both fragments
    override fun onAttach(context: Context) {
        super.onAttach(context)
        println("onAttach: ${this.javaClass.simpleName} tag: ${this.tag}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("onCreate: ${this.javaClass.simpleName} tag: ${this.tag}")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        println("onDestroyView: ${this.javaClass.simpleName} tag: ${this.tag}")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy: ${this.javaClass.simpleName} tag: ${this.tag}")
    }

}
