package com.example.firedatabase_assis.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
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
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SearchViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poster, container, false)

        // Initialize your views
        val titleTextView: TextView = view.findViewById(R.id.title)
        val overviewTextView: TextView = view.findViewById(R.id.caption)
        viewPager = view.findViewById(R.id.slider)

        // Get the item details from the ViewModel
        viewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            when (item) {
                is Movie -> {
                    titleTextView.text = item.title
                    overviewTextView.text = item.overview
                    setupViewPager(item.poster_path, item.id, "movie")
                }

                is TV -> {
                    titleTextView.text = item.name
                    overviewTextView.text = item.overview
                    setupViewPager(item.poster_path, item.id, "tv")
                }
            }
        }

        return view
    }

    private fun setupViewPager(posterPath: String?, id: Int, type: String) {
        val items = mutableListOf<Any>()

        // Load the poster image first
        posterPath?.let {
            items.add(it)
        }

        // Fetch and add the first video key
        fetchVideos(id, type) { videoKey ->
            if (videoKey != null) {
                items.add(videoKey)
            }
            viewPager.adapter = PosterViewPagerAdapter(items)
        }
    }

    private fun fetchVideos(id: Int, type: String, callback: (String?) -> Unit) {
        val url = if (type == "movie") {
            "https://api.themoviedb.org/3/movie/$id/videos?language=en-US"
        } else {
            "https://api.themoviedb.org/3/tv/$id/videos?language=en-US"
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json")
            .addHeader(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOWRkOWQzYWU4MzhkYjE4ZDUxZjg4Y2Q1MGU0NzllNCIsIm5iZiI6MTcyMTA4Mzk5MS42ODc5NTUsInN1YiI6IjY2MjZiM2ZkMjU4ODIzMDE2NDkxODliMSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.dXcgTC2h_FTmM94Xx-pE04jF3F8tPoFYBxcKMmnV338"
            )
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    val jsonObject = JSONObject(jsonData)
                    val results = jsonObject.getJSONArray("results")
                    val videoKey = results.getJSONObject(0).getString("key")
                    withContext(Dispatchers.Main) {
                        callback(videoKey)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
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
                    .load("https://image.tmdb.org/t/p/w500$posterPath")
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
}