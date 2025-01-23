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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PosterFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var castRecyclerView: RecyclerView
    private lateinit var castAdapter: CastAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("onCreate: ${this.javaClass.simpleName} tag: ${this.tag}")
        viewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poster, container, false)
        println("onCreateView: ${this.javaClass.simpleName} tag: ${this.tag}")
        setupViews(view)
        setupObservers()
        return view
    }

    private fun setupViews(view: View) {

        view.findViewById<ImageView>(R.id.back).setOnClickListener {
            viewModel.navigate(SearchViewModel.NavigationState.Back)
        }

        view.findViewById<ImageView>(R.id.close).setOnClickListener {
            viewModel.navigate(SearchViewModel.NavigationState.Close)
        }


        viewPager = view.findViewById(R.id.slider)
        indicatorContainer = view.findViewById(R.id.indicatorContainer)

        // Setup cast RecyclerView
        castAdapter = CastAdapter { castMember ->
            viewModel.navigate(SearchViewModel.NavigationState.ShowPerson(castMember.id))
        }


        castRecyclerView = view.findViewById(R.id.recyclerViewProfiles)
        castRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        castRecyclerView.adapter = castAdapter

        // Set up ViewPager page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
            }
        })
    }

    private fun setupObservers() {
        viewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            when (item) {
                is Movie -> handleMovieItem(item)
                is TV -> handleTVItem(item)
            }
        }
    }

    private fun handleMovieItem(movie: Movie) {
        view?.apply {
            findViewById<TextView>(R.id.title).text = movie.title
            findViewById<TextView>(R.id.caption).text = movie.overview
            setupViewPager(movie.poster_path, movie.id, "movie")
            fetchCast(movie.id, "movie")
        }
    }

    private fun handleTVItem(tv: TV) {
        view?.apply {
            findViewById<TextView>(R.id.title).text = tv.name
            findViewById<TextView>(R.id.caption).text = tv.overview
            setupViewPager(tv.poster_path, tv.id, "tv")
            fetchCast(tv.id, "tv")
        }
    }

    private fun setupViewPager(posterPath: String?, id: Int, type: String) {
        val items = mutableListOf<Any>()

        // Load the poster image first
        posterPath?.let { items.add(it) }

        // Fetch and add the first video key
        fetchVideos(id, type) { videoKey ->
            videoKey?.let { items.add(it) }
            viewPager.adapter = PosterViewPagerAdapter(items)
            setupIndicators(items.size)
            updateIndicators(0)
        }
    }

    private fun fetchCast(id: Int, type: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = if (type == "movie") {
                    "https://api.themoviedb.org/3/movie/$id/credits?language=en-US"
                } else {
                    "https://api.themoviedb.org/3/tv/$id/credits?language=en-US"
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = response.body?.string()
                        val castList = parseCast(jsonData, type)
                        withContext(Dispatchers.Main) {
                            if (isAdded) castAdapter.submitList(castList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseCast(jsonData: String?, type: String): List<CastMember> {
        val castList = mutableListOf<CastMember>()
        jsonData?.let {
            val jsonObject = JSONObject(it)
            val castArray = jsonObject.getJSONArray("cast")
            for (i in 0 until castArray.length()) {
                val castObject = castArray.getJSONObject(i)
                castObject.optString("profile_path").let { path ->
                    castList.add(
                        CastMember(
                            id = castObject.getInt("id"),
                            name = castObject.getString("name"),
                            character = castObject.getString("character"),
                            episodeCount = if (type == "tv") castObject.optInt(
                                "episode_count",
                                0
                            ) else 1,
                            profilePath = path
                        )
                    )
                }
            }
        }
        return castList
    }

    private fun fetchVideos(id: Int, type: String, callback: (String?) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = if (type == "movie") {
                    "https://api.themoviedb.org/3/movie/$id/videos?language=en-US"
                } else {
                    "https://api.themoviedb.org/3/tv/$id/videos?language=en-US"
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = response.body?.string()
                        val videoKey = parseVideoKey(jsonData)
                        withContext(Dispatchers.Main) { callback(videoKey) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    private fun parseVideoKey(jsonData: String?): String? {
        jsonData?.let {
            val jsonObject = JSONObject(it)
            val results = jsonObject.getJSONArray("results")
            if (results.length() > 0) {
                return results.getJSONObject(0).getString("key")
            }
        }
        return null
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


    // Add to both fragments
    override fun onAttach(context: Context) {
        super.onAttach(context)
        println("onAttach: ${this.javaClass.simpleName} tag: ${this.tag}")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        println("onDestroyView: ${this.javaClass.simpleName} tag: ${this.tag}")
    }


    inner class PosterViewPagerAdapter(private val items: List<Any>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image, parent, false)
                ImageViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_video, parent, false)
                VideoViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == 0) {
                (holder as ImageViewHolder).bind(items[position] as String)
            } else {
                (holder as VideoViewHolder).bind(items[position] as String)
            }
        }

        override fun getItemCount(): Int = items.size

        override fun getItemViewType(position: Int): Int {
            return if (items[position] is String && (items[position] as String).startsWith("/")) 0 else 1
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
        fun newInstance(item: Any): PosterFragment {
            return PosterFragment().apply {
                arguments = Bundle().apply {
                    when (item) {
                        is Movie -> putParcelable("movie", item)
                        is TV -> putParcelable("tv", item)
                    }
                }
            }
        }

        fun getFragmentTag(item: Any): String {
            return when (item) {
                is Movie -> "poster_movie_${item.id}"
                is TV -> "poster_tv_${item.id}"
                else -> "unknown_item"
            }
        }
    }
}