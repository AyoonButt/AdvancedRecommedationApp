package com.example.firedatabase_assis.search

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.workers.Video
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PosterFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: CastAdapter
    private lateinit var recommendationsAdapter: MediaItemAdapter
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val postsApi = retrofit.create(Posts::class.java)
    private val client = OkHttpClient()

    private var startTimestamp: String? = null
    private var isBackNavigation = false


    override fun onAttach(context: Context) {
        super.onAttach(context)
        println("onAttach: ${this.javaClass.simpleName} tag: ${this.tag}")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("onCreate: ${this.javaClass.simpleName} tag: ${this.tag}")
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
        userViewModel = UserViewModel.getInstance(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        startTimestamp =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val view = inflater.inflate(R.layout.fragment_poster, container, false)
        println("onCreateView: ${this.javaClass.simpleName} tag: ${this.tag}")
        setupViews(view)

        arguments?.let { args ->
            val id = args.getInt("id")
            val isMovie = args.getBoolean("isMovie", false)
            val fetchData: (JSONObject) -> Unit = { data -> updateUI(data, isMovie) }
            if (isMovie) fetchMovieData(id, fetchData) else fetchTVData(id, fetchData)
        }
        return view
    }

    private fun setupViews(view: View) {

        view.findViewById<ImageView>(R.id.back).setOnClickListener {
            isBackNavigation = true  // Set flag when back is clicked
            viewModel.navigate(SearchViewModel.NavigationState.Back)
        }

        view.findViewById<ImageView>(R.id.close).setOnClickListener {
            viewModel.navigate(SearchViewModel.NavigationState.Close)
        }

        viewPager = view.findViewById(R.id.slider)
        indicatorContainer = view.findViewById(R.id.indicatorContainer)

        // Setup cast RecyclerView
        castAdapter = CastAdapter { castMember ->
            viewModel.setSelectedItem(castMember)
            viewModel.navigate(SearchViewModel.NavigationState.ShowPerson(castMember.id))
        }

        castRecyclerView = view.findViewById(R.id.recyclerViewProfiles)
        castRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        castRecyclerView.adapter = castAdapter


        recommendationsAdapter = MediaItemAdapter(
            onItemClick = { item ->
                if (!viewModel.isLoading.value) {
                    viewModel.setSelectedItem(item) // Set the selected item in the view model

                    // Check if the item is a Movie or TV and pass the isMovie flag
                    val isMovie = item is Movie

                    // Navigate to show the poster with the corresponding flag
                    viewModel.navigate(
                        SearchViewModel.NavigationState.ShowPoster(
                            item.id,
                            isMovie
                        )
                    )
                }
            },
            isRecommendation = true
        )

        val space = resources.getDimensionPixelSize(R.dimen.item_spacing)

        val recyclerViewRecommendations =
            view.findViewById<RecyclerView>(R.id.recyclerViewRecommendations)
        recyclerViewRecommendations.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewRecommendations.addItemDecoration(MediaItemAdapter.ItemSpacingDecoration(space))
        recyclerViewRecommendations.adapter = recommendationsAdapter


        // Set up ViewPager page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
            }
        })


    }


    private fun fetchMovieData(id: Int, updateUI: (JSONObject) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url =
                    "https://api.themoviedb.org/3/movie/$id?append_to_response=credits,videos,recommendations"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = JSONObject(response.body?.string())
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            updateUI(jsonData)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchTVData(id: Int, updateUI: (JSONObject) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url =
                    "https://api.themoviedb.org/3/tv/$id?append_to_response=aggregate_credits,videos,recommendations"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = JSONObject(response.body?.string())
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            updateUI(jsonData)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(data: JSONObject, isMovie: Boolean) {
        view?.apply {
            // Basic Info
            val title = if (isMovie) data.getString("title") else data.getString("name")
            findViewById<TextView>(R.id.title).apply {
                text = title
                visibility = if (title.isNotEmpty()) View.VISIBLE else View.GONE
            }

            val overview = data.getString("overview")
            findViewById<TextView>(R.id.caption).apply {
                text = overview
                visibility = if (overview.isNotEmpty()) View.VISIBLE else View.GONE
            }
            findViewById<TextView>(R.id.overviewLabel).visibility =
                if (overview.isNotEmpty()) View.VISIBLE else View.GONE

            // Movie-specific fields
            val releaseDate =
                if (isMovie) data.getString("release_date") else data.getString("first_air_date")
            findViewById<TextView>(R.id.releaseDate).text = releaseDate
            findViewById<LinearLayout>(R.id.releaseDateContainer).visibility =
                if (releaseDate.isNotEmpty()) View.VISIBLE else View.GONE

            val runtimeValue = if (isMovie) {
                data.getInt("runtime")
            } else {
                data.getJSONArray("episode_run_time").let {
                    if (it.length() > 0) it.getInt(0) else 0
                }
            }

            findViewById<TextView>(R.id.runtime).text =
                if (runtimeValue > 0) "$runtimeValue min" else ""
            findViewById<LinearLayout>(R.id.runtimeContainer).visibility =
                if (runtimeValue > 0) View.VISIBLE else View.GONE

            val actualTmdbId = data.getInt("id")
            lifecycleScope.launch {
                val provider = fetchProviderForTmdbId(actualTmdbId)
                findViewById<TextView>(R.id.subscription).text = provider ?: ""
                findViewById<LinearLayout>(R.id.subscriptionContainer).visibility =
                    if (provider != null) View.VISIBLE else View.GONE
            }

            // TV-specific fields
            findViewById<LinearLayout>(R.id.tvSpecificContainer).visibility =
                if (!isMovie) View.VISIBLE else View.GONE

            if (!isMovie) {
                val lastAirDate =
                    if (data.isNull("last_air_date")) "Current" else data.getString("last_air_date")
                findViewById<TextView>(R.id.lastAirDate).text = lastAirDate
                findViewById<LinearLayout>(R.id.lastAirDateContainer).visibility =
                    if (lastAirDate.isNotEmpty()) View.VISIBLE else View.GONE

                val productionStatus =
                    if (data.getBoolean("in_production")) "In Production" else "Ended"
                findViewById<TextView>(R.id.inProduction).text = productionStatus
                findViewById<LinearLayout>(R.id.statusContainer).visibility =
                    if (productionStatus.isNotEmpty()) View.VISIBLE else View.GONE

                val nextEpisode = data.optJSONObject("next_episode_to_air")?.getString("name")
                    ?: "No upcoming episodes"
                findViewById<TextView>(R.id.nextEpisode).text = nextEpisode
                findViewById<LinearLayout>(R.id.nextEpisodeContainer).visibility =
                    if (nextEpisode.isNotEmpty()) View.VISIBLE else View.GONE

                val numEpisodes = data.getInt("number_of_episodes")
                val episodesText = if (numEpisodes > 0) "$numEpisodes episodes" else ""
                findViewById<TextView>(R.id.numberOfEpisodes).text = episodesText
                findViewById<LinearLayout>(R.id.episodesContainer).visibility =
                    if (episodesText.isNotEmpty()) View.VISIBLE else View.GONE

                val numSeasons = data.getInt("number_of_seasons")
                val seasonsText = if (numSeasons > 0) "$numSeasons seasons" else ""
                findViewById<TextView>(R.id.numberOfSeasons).text = seasonsText
                findViewById<LinearLayout>(R.id.seasonsContainer).visibility =
                    if (seasonsText.isNotEmpty()) View.VISIBLE else View.GONE
            }

            // Common fields
            val genresList = data.getJSONArray("genres")
                .let { 0.until(it.length()).map { i -> it.getJSONObject(i).getString("name") } }
            val genres = genresList.joinToString(", ")
            findViewById<TextView>(R.id.genres).text = genres
            findViewById<LinearLayout>(R.id.genresContainer).visibility =
                if (genres.isNotEmpty()) View.VISIBLE else View.GONE

            val countriesList = data.getJSONArray("origin_country")
                .let { 0.until(it.length()).map { i -> it.getString(i) } }
            val originCountries = countriesList.joinToString(", ")
            findViewById<TextView>(R.id.countries).text = originCountries
            findViewById<LinearLayout>(R.id.countriesContainer).visibility =
                if (originCountries.isNotEmpty()) View.VISIBLE else View.GONE

            val companiesList = data.getJSONArray("production_companies")
                .let { 0.until(it.length()).map { i -> it.getJSONObject(i).getString("name") } }
            val companies = companiesList.joinToString(", ")
            findViewById<TextView>(R.id.companies).text = companies
            findViewById<LinearLayout>(R.id.companiesContainer).visibility =
                if (companies.isNotEmpty()) View.VISIBLE else View.GONE

            val collection = data.optJSONObject("belongs_to_collection")?.getString("name") ?: ""
            findViewById<TextView>(R.id.collection).text = collection
            findViewById<LinearLayout>(R.id.collectionContainer).visibility =
                if (collection.isNotEmpty()) View.VISIBLE else View.GONE

            // Media content
            val posterPath = data.getString("poster_path")
            setupViewPager(posterPath, selectBestVideoKey(data.getJSONObject("videos")))

            // Cast - use different fields based on type
            val cast = if (isMovie) {
                parseCast(data.getJSONObject("credits"))
            } else {
                parseTVCast(data.getJSONObject("aggregate_credits"))
            }
            castAdapter.submitList(cast)

            findViewById<TextView>(R.id.castLabel).visibility =
                if (cast.isNotEmpty()) View.VISIBLE else View.GONE
            findViewById<RecyclerView>(R.id.recyclerViewProfiles).visibility =
                if (cast.isNotEmpty()) View.VISIBLE else View.GONE

            // Recommendations
            val recommendations = data.getJSONObject("recommendations")
                .getJSONArray("results")
                .let { array ->
                    (0 until array.length()).map { i ->
                        val item = array.getJSONObject(i)
                        val mediaType = item.getString("media_type")

                        when (mediaType) {
                            "movie" -> Movie(
                                id = item.getInt("id"),
                                title = item.getString("title"),
                                poster_path = item.optString("poster_path", null),
                                overview = item.getString("overview"),
                                mediaType = "movie",
                                originalLanguage = item.getString("original_language"),
                                originalTitle = item.getString("original_title"),
                                popularity = item.getDouble("popularity"),
                                voteAverage = item.getDouble("vote_average"),
                                firstAirDate = item.optString("release_date", ""),
                                voteCount = item.getInt("vote_count"),
                                genreIds = item.getJSONArray("genre_ids")
                                    .let { genreArray ->
                                        (0 until genreArray.length()).map {
                                            genreArray.getInt(
                                                it
                                            )
                                        }
                                    }
                            )

                            "tv" -> TV(
                                id = item.getInt("id"),
                                title = item.getString("name"),
                                poster_path = item.optString("poster_path", null),
                                overview = item.getString("overview"),
                                mediaType = "tv",
                                originalLanguage = item.getString("original_language"),
                                originalTitle = item.getString("original_name"),
                                popularity = item.getDouble("popularity"),
                                voteAverage = item.getDouble("vote_average"),
                                firstAirDate = item.optString("first_air_date", ""),
                                voteCount = item.getInt("vote_count"),
                                genreIds = item.getJSONArray("genre_ids")
                                    .let { genreArray ->
                                        (0 until genreArray.length()).map {
                                            genreArray.getInt(
                                                it
                                            )
                                        }
                                    }
                            )

                            else -> null
                        }
                    }.filterNotNull()
                }
            recommendationsAdapter.submitList(recommendations)

            findViewById<TextView>(R.id.recommendationsLabel).visibility =
                if (recommendations.isNotEmpty()) View.VISIBLE else View.GONE
            findViewById<RecyclerView>(R.id.recyclerViewRecommendations).visibility =
                if (recommendations.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }


    private fun setupViewPager(posterPath: String?, videoKey: String?) {
        val items = mutableListOf<ViewPagerItem>()
        posterPath?.let { items.add(ViewPagerItem.Image(it)) }
        videoKey?.let { items.add(ViewPagerItem.Video(it)) }

        viewPager.adapter = PosterViewPagerAdapter(items)
        setupIndicators(items.size)
        updateIndicators(0)
    }

    private fun selectBestVideoKey(videosJson: JSONObject): String? {
        println("Selecting the best video key from results: $videosJson")

        // Define the priority order for video types
        val priorityOrder = listOf("Short", "Trailer", "Teaser", "Featurette", "Clip")

        // Extract the videos array from the JSON object
        val videos = videosJson.getJSONArray("results")
        val videoList = mutableListOf<Video>()

        // Parse the JSON array into a list of Video objects
        for (i in 0 until videos.length()) {
            val videoObject = videos.getJSONObject(i)
            val video = Video(
                key = videoObject.getString("key"),
                type = videoObject.getString("type"),
                isOfficial = videoObject.optBoolean("official", false),
                publishedAt = videoObject.optString("published_at") // Adjust the key based on your JSON structure
            )
            videoList.add(video)
        }

        // Filter and sort videos based on priority and official status
        val filteredVideos = videoList
            .filter { video ->
                video.isOfficial == true && priorityOrder.contains(video.type)
            }
            .sortedWith(
                compareBy(
                    { priorityOrder.indexOf(it.type) },  // Prioritize by type
                    { it.publishedAt ?: "" }  // Handle null values
                )
            )

        // If no official videos found, look for unofficial trailers or most recently published videos
        val bestVideo = filteredVideos.lastOrNull()
            ?: videoList
                .filter { video ->
                    video.type == "Trailer" && !video.isOfficial!!
                }
                .maxByOrNull { it.publishedAt ?: "" }  // Handle null values
            ?: videoList.maxByOrNull { it.publishedAt ?: "" }  // Handle null values

        // Return the selected video's key or null if no videos are available
        return bestVideo?.key
    }

    private fun parseCast(jsonData: JSONObject?): List<CastMember> {
        val castList = mutableListOf<CastMember>()
        jsonData?.let { jsonObject ->
            val castArray = jsonObject.getJSONArray("cast")
            for (i in 0 until castArray.length()) {
                val castObject = castArray.getJSONObject(i)
                castObject.optString("profile_path").let { path ->
                    castList.add(
                        CastMember(
                            id = castObject.getInt("id"),
                            name = castObject.getString("name"),
                            character = castObject.getString("character"),
                            episodeCount = 1,
                            profilePath = path
                        )
                    )
                }
            }
        }
        return castList
    }

    private fun parseTVCast(jsonData: JSONObject?): List<CastMember> {
        val castList = mutableListOf<CastMember>()

        jsonData?.let { jsonObject ->
            // Parse the cast data
            val castArray = jsonObject.getJSONArray("cast")
            for (i in 0 until castArray.length()) {
                val castObject = castArray.getJSONObject(i)

                // Get profile path, or null if not available
                val profilePath = castObject.optString("profile_path", null)

                // Get roles array for character name and episode count
                val rolesArray = castObject.getJSONArray("roles")

                if (rolesArray.length() > 0) {
                    val roleObject = rolesArray.getJSONObject(0)
                    val character = roleObject.getString("character")
                    val episodeCount = roleObject.getInt("episode_count")

                    castList.add(
                        CastMember(
                            id = castObject.getInt("id"),
                            name = castObject.getString("name"),
                            character = character,
                            episodeCount = episodeCount,
                            profilePath = profilePath
                        )
                    )
                } else {
                    // Fallback if no roles information is available
                    castList.add(
                        CastMember(
                            id = castObject.getInt("id"),
                            name = castObject.getString("name"),
                            character = "",
                            episodeCount = 0,
                            profilePath = profilePath
                        )
                    )
                }
            }
        }
        return castList
    }

    private suspend fun fetchProviderForTmdbId(tmdbId: Int): String? {
        return try {
            val response = postsApi.getProviderNameByTmdbId(tmdbId)
            if (response.isSuccessful) {
                response.body()?.get("provider")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("API", "Error fetching provider for TMDB ID $tmdbId", e)
            null
        }
    }


    private fun setupIndicators(count: Int) {
        indicatorContainer.removeAllViews()
        if (count <= 1) return

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,

            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(8, 0, 8, 0) }

        repeat(count) {
            val dot = ImageView(requireContext())
            dot.setImageResource(R.drawable.dot_unselected)
            indicatorContainer.addView(dot, params)
        }
    }

    private fun updateIndicators(position: Int) {
        val count = indicatorContainer.childCount
        for (i in 0 until count) {
            val dot = indicatorContainer.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.dot_selected else R.drawable.dot_unselected)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("onViewCreated: ${this.javaClass.simpleName} tag: ${this.tag}")
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

    private fun saveCurrentViewDuration() {
        arguments?.let { args ->
            val id = args.getInt("id")
            val isMovie = args.getBoolean("isMovie", false)
            startTimestamp?.let { startTime ->
                val endTimestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

                userViewModel.currentUser.value?.userId?.let { userId ->
                    viewModel.saveViewDuration(
                        tmdbId = id,
                        type = if (isMovie) "movie" else "tv",
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

    inner class PosterViewPagerAdapter(private val items: List<ViewPagerItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> ImageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_image, parent, false)
                )

                1 -> VideoViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_video, parent, false)
                )

                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is ViewPagerItem.Image -> (holder as ImageViewHolder).bind(item.path)
                is ViewPagerItem.Video -> (holder as VideoViewHolder).bind(item.key)
            }
        }

        override fun getItemCount(): Int = items.size

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is ViewPagerItem.Image -> 0
            is ViewPagerItem.Video -> 1
        }


        inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val imageView: ImageView = view.findViewById(R.id.imageView)

            fun bind(posterPath: String) {
                Glide.with(imageView.context)
                    .load("https://image.tmdb.org/t/p/original$posterPath")
                    .into(imageView)
            }
        }

        inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val youTubePlayerView: YouTubePlayerView =
                view.findViewById(R.id.youtube_player_view)

            fun bind(videoKey: String) {
                lifecycle.addObserver(youTubePlayerView)
                youTubePlayerView.addYouTubePlayerListener(object :
                    AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoKey, 0f)
                    }
                })
            }
        }
    }

    companion object {
        fun newInstance(id: Int, isMovie: Boolean): PosterFragment {
            return PosterFragment().apply {
                arguments = Bundle().apply {
                    putInt("id", id)
                    putBoolean("isMovie", isMovie)
                }
            }
        }
    }

}