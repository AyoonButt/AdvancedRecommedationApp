package com.example.firedatabase_assis.search

import android.content.Context
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
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

class PosterFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: CastAdapter
    private lateinit var recommendationsAdapter: MediaItemAdapter
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
            findViewById<TextView>(R.id.title).text =
                if (isMovie) data.getString("title") else data.getString("name")
            findViewById<TextView>(R.id.caption).text = data.getString("overview")

            // Movie-specific fields
            findViewById<TextView>(R.id.releaseDate).apply {
                text =
                    if (isMovie) data.getString("release_date") else data.getString("first_air_date")
            }
            findViewById<TextView>(R.id.runtime).apply {
                text = if (isMovie) "${data.getInt("runtime")} min" else "${
                    data.getJSONArray("episode_run_time").let {
                        if (it.length() > 0) it.getInt(0) else 0
                    }
                } min"
            }

            // TV-specific fields
            findViewById<TextView>(R.id.lastAirDate).apply {
                visibility = if (!isMovie) View.VISIBLE else View.GONE
                text = if (!isMovie) {
                    if (data.isNull("last_air_date")) "Current" else data.getString("last_air_date")
                } else null
            }

            findViewById<TextView>(R.id.inProduction).apply {
                visibility = if (!isMovie) View.VISIBLE else View.GONE
                text = if (!isMovie) {
                    if (data.getBoolean("in_production")) "In Production" else "Ended"
                } else null
            }

            findViewById<TextView>(R.id.nextEpisode).apply {
                visibility = if (!isMovie) View.VISIBLE else View.GONE
                text = if (!isMovie) {
                    data.optJSONObject("next_episode_to_air")?.getString("name")
                        ?: "No upcoming episodes"
                } else null
            }

            findViewById<TextView>(R.id.numberOfEpisodes).apply {
                visibility = if (!isMovie) View.VISIBLE else View.GONE
                text = if (!isMovie) "${data.getInt("number_of_episodes")} episodes" else null
            }

            findViewById<TextView>(R.id.numberOfSeasons).apply {
                visibility = if (!isMovie) View.VISIBLE else View.GONE
                text = if (!isMovie) "${data.getInt("number_of_seasons")} seasons" else null
            }

            // Common fields
            val genres = data.getJSONArray("genres")
                .let { 0.until(it.length()).map { i -> it.getJSONObject(i).getString("name") } }
                .joinToString(", ")
            findViewById<TextView>(R.id.genres).text = genres

            val originCountries = data.getJSONArray("origin_country")
                .let { 0.until(it.length()).map { i -> it.getString(i) } }
                .joinToString(", ")
            findViewById<TextView>(R.id.countries).text = originCountries

            val companies = data.getJSONArray("production_companies")
                .let { 0.until(it.length()).map { i -> it.getJSONObject(i).getString("name") } }
                .joinToString(", ")
            findViewById<TextView>(R.id.companies).text = companies

            findViewById<TextView>(R.id.collection).apply {
                visibility =
                    data.optJSONObject("belongs_to_collection")?.let { View.VISIBLE } ?: View.GONE
                text = data.optJSONObject("belongs_to_collection")?.getString("name")
            }

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

                // Get roles and episode count (assuming roles array is present)
                val rolesArray = castObject.getJSONArray("roles")
                val episodeCount = if (rolesArray.length() > 0) {
                    rolesArray.getJSONObject(0).getInt("episode_count")
                } else {
                    0 // Default to 0 if there are no roles
                }

                // Create and add CastMember to the list
                castList.add(
                    CastMember(
                        id = castObject.getInt("id"),
                        name = castObject.getString("name"),
                        character = castObject.getString("roles").takeIf { it.isNotEmpty() }
                            ?: "Unknown",  // Character can be empty, hence a fallback
                        episodeCount = episodeCount,
                        profilePath = profilePath
                    )
                )
            }
        }
        return castList
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