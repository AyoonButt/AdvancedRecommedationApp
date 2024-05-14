package com.example.firedatabase_assis


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.Cache
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.example.firedatabase_assis.databinding.ActivityHomepageBinding
import kotlinx.coroutines.*
import java.util.PriorityQueue
import kotlin.coroutines.CoroutineContext


class HomePage : AppCompatActivity(), CoroutineScope {
    private lateinit var binding: ActivityHomepageBinding
    private val imageContainers: PriorityQueue<ImageContainerWrapper> =
        PriorityQueue() // Stores wrappers with priority and container data
    private lateinit var requestQueue: RequestQueue
    private val containerTagsMap: MutableMap<String, ContainerTags> = HashMap()

    // Coroutine Job to handle the loop
    private var loopJob: Job? = null

    // Coroutine context
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + SupervisorJob()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestQueue = Volley.newRequestQueue(this)
        startLoop()
        //loadContainersFromQueue()


        // move to a separate view
        binding.bottomNavBar.setOnItemReselectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_menu_home -> {
                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                }

                R.id.bottom_menu_explore -> {
                    val intent = Intent(this, ReviewActivity::class.java)
                    startActivity(intent)
                }

                R.id.bottom_menu_communities -> {
                    val intent = Intent(this, CommunitiesActivity::class.java)
                    startActivity(intent)
                }

                R.id.bottom_menu_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun startLoop() {
        loopJob = launch {
            repeat(10) {
                loadImageWithDelay()
                delay(15L)
            }
        }
    }

    private fun updateAndAddToQueue(wrapper: ImageContainerWrapper) {
        wrapper.updatePriority() // Update the priority based on conditions
        imageContainers.add(wrapper) // Add to the priority queue

    }

    private suspend fun loadImageWithDelay() {
        withContext(Dispatchers.IO) {
            val dbHelper = MediaDBHelper(applicationContext)
            val containerLayout = findViewById<LinearLayout>(R.id.containerLayout)

            val source = "imdb_id"
            val url = "https://api.themoviedb.org/3/find/"
            val key = "d9dd9d3ae838db18d51f88cd50e479e4"
            val token =
                "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOWRkOWQzYWU4MzhkYjE4ZDUxZjg4Y2Q1MGU0NzllNCIsInN1YiI6IjY2MjZiM2ZkMjU4ODIzMDE2NDkxODliMSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.wIF16waIO_pGrRKxWr4ye8QFYUFMGP6WBDX5Wg2JOpM"

            val externalId = dbHelper.getLastImdbIdFromDatabase(this@HomePage)
            Log.d("ExternalID", "External ID: $externalId")

            val caption = dbHelper.getCaption(applicationContext)

            // Create the request
            val request = object : StringRequest(
                Method.GET,
                "$url$externalId?api_key=$key&external_source=$source",

                Response.Listener { response ->
                    Log.d("RequestURL", "$url$externalId?external_source=$source")

                    val poster = parseResponse(response)
                    val baseURL = "https://image.tmdb.org/t/p/w185$poster"

                    // Inflate the image container layout
                    val imageContainer =
                        layoutInflater.inflate(R.layout.item_image_container, null) as LinearLayout
                    val imageView = imageContainer.findViewById<ImageView>(R.id.imageView)
                    val btnLike = imageContainer.findViewById<ToggleButton>(R.id.btnLike)
                    val btnDislike = imageContainer.findViewById<ToggleButton>(R.id.btnDislike)
                    val btnSaved = imageContainer.findViewById<ToggleButton>(R.id.btnSaved)
                    val captionTextView =
                        imageContainer.findViewById<TextView>(R.id.captionTextView)

                    // Parse the captionString to extract relevant information
                    val parts = caption.split("\n")

                    var title = ""
                    var overview = ""
                    var streamingService = ""
                    var quality = ""
                    //var link = ""
                    //var videoLink = ""
                    var year = ""
                    //var imdbid = ""
                    //var tmdbid = ""
                    var genres = ""
                    var directors = ""
                    var creators = ""

                    for (part in parts) {
                        val keyValue = part.split(":")
                        if (keyValue.size == 2) {
                            val key = keyValue[0].trim()
                            val value = keyValue[1].trim()
                            when (key) {
                                "title" -> title = value
                                "overview" -> overview = value
                                "streamingService" -> streamingService = value
                                "quality" -> quality = value
                                //"link" -> link = value
                                //"videoLink" -> videoLink = value
                                "year" -> year = value
                                //"imdbid" -> imdbid = value
                                //"tmdbid" -> tmdbid = value
                                "genres" -> genres = value
                                "directors" -> directors = value
                                "creators" -> creators = value
                            }
                        }
                    }
                    val formattedGenres = genres.removeSurrounding("[", "]")
                        .replace("Genre(id=", "")
                        .replace(
                            "\\d+, name=".toRegex(),
                            ""
                        ) // Remove numbers followed by ', name='
                        .replace(")", "")
                        .trim() // Trims any leading or trailing whitespace
                        .replace(", ", ", ") // Ensure consistent comma spacing
                    // Trim square brackets and format directors
                    val formattedDirectors =
                        directors.removeSurrounding("[", "]").replace(",", ", ")
                    val formattedCreators = creators.removeSurrounding("[", "]").replace(",", ", ")
                    // Build the formatted information
                    val formattedInfo = buildString {
                        appendFormattedField(this, "Title:", title)
                        appendFormattedField(this, "Overview:", overview)
                        appendFormattedField(this, "Streaming Service:", streamingService)
                        appendFormattedField(this, "Quality:", quality)
                        appendFormattedField(this, "Year:", year)
                        appendFormattedField(this, "Genres:", formattedGenres)
                        appendFormattedField(this, "Directors:", formattedDirectors)
                        appendFormattedField(this, "Creators:", formattedCreators)
                    }

                    // Display the formatted information
                    Log.d("FormattedInfo", formattedInfo)

                    // Set the formatted caption to the TextView
                    captionTextView.text = formattedInfo
                    val serviceTag = extractStreamingService(caption)


                    val genreTag = extractGenres(caption) //List of Strings



                    containerTagsMap[containerLayout.id.toString()] =
                        ContainerTags(serviceTag, genreTag)

                    try {
                        loadImageFromUrl(baseURL, imageView)
                    } catch (e: GlideException) {
                        e.logRootCauses("GlideException")
                    }

                    // Handle like button click
                    btnLike.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            // If like button is checked, ensure dislike button is unchecked
                            btnDislike.isChecked = false
                            // Update icon based on liked state
                            btnLike.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.icon_thumb_up_filled, // Use your filled icon drawable
                                0,
                                0,
                                0
                            )
                        } else {
                            // If like button is unchecked, revert to the default icon
                            btnLike.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.icon_thumb_up_unfilled, // Use your unfilled icon drawable
                                0,
                                0,
                                0
                            )
                        }
                        val containerId = containerLayout.id.toString()
                        val containerTags =
                            containerTagsMap[containerId] ?: ContainerTags(null, null)

                        containerTags.likeState = isChecked // Update liked state

                        // Update the containerTagsMap
                        containerTagsMap[containerId] = containerTags


                    }

                    // Handle dislike button click
                    btnDislike.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            // If dislike button is checked, ensure like button is unchecked
                            btnLike.isChecked = false
                            // Update icon based on disliked state
                            btnDislike.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.icon_thumb_down_filled, // Use your filled icon drawable
                                0,
                                0,
                                0
                            )
                        } else {
                            // If dislike button is unchecked, revert to the default icon
                            btnDislike.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.icon_thumb_down_unfilled, // Use your unfilled icon drawable
                                0,
                                0,
                                0
                            )
                        }
                        val containerId = containerLayout.id.toString()
                        val containerTags =
                            containerTagsMap[containerId] ?: ContainerTags(null, null)

                        containerTags.dislikeState = isChecked // Update liked state

                        // Update the containerTagsMap
                        containerTagsMap[containerId] = containerTags
                    }

                    // Handle saved button click
                    btnSaved.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            // If saved button is checked, ensure both like and dislike buttons are unchecked
                            btnLike.isChecked = false
                            btnDislike.isChecked = false
                            // Update icon based on saved state
                            btnSaved.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.icon_bookmark_filled, // Use your filled icon drawable
                                0,
                                0,
                                0
                            )
                        } else {
                            // If saved button is unchecked, revert to the default icon
                            btnSaved.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.icon_bookmark_unfilled, // Use your unfilled icon drawable
                                0,
                                0,
                                0
                            )
                        }
                        val containerId = containerLayout.id.toString()
                        val containerTags =
                            containerTagsMap[containerId] ?: ContainerTags(null, null)

                        containerTags.savedState = isChecked // Update liked state

                        // Update the containerTagsMap
                        containerTagsMap[containerId] = containerTags
                    }


                    // Add the image container to the layout
                    containerLayout.addView(imageContainer)

                    // Create an ImageContainerWrapper instance
                    val wrapper = ImageContainerWrapper(

                        priority = 0, // Set initial priority (can be updated later)
                        imageView = imageView,
                        btnLike = btnLike,
                        btnDislike = btnDislike,
                        btnSaved = btnSaved,
                        captionTextView = captionTextView,
                        containerLayoutId = containerLayout.id.toString(),
                        containerTagsMap = containerTagsMap,
                        imageUrl = baseURL
                    )

                    Log.d("BaseURL", "Base URL added: ${wrapper.imageUrl}")
                    // Update priority and add to the priority queue
                    wrapper.updatePriority() // Update priority based on tags
                    updateAndAddToQueue(wrapper)


                },

                Response.ErrorListener { error ->
                    // Handle error
                }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["accept"] = "application/json"
                    headers["Authorization"] = "Bearer $token"
                    return headers
                }

                override fun getCacheEntry(): Cache.Entry? {
                    return null
                }
            }

            // Add the request to the request queue
            requestQueue.add(request)
            Log.d("Volley", "Added request to queue")

            val last = dbHelper.getLastImdbIdFromDatabase(this@HomePage)
            if (last != null) {
                dbHelper.deleteRowByExternalId(this@HomePage, last)
            }
        }
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        Glide.with(imageView)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()
            .placeholder(R.drawable.lotr) // Placeholder image while loading
            .into(imageView)

    }

    private fun loadContainersFromQueue() {
        launch {

            while (imageContainers.isNotEmpty()) {
                Log.d("Caption", "Hi: ")
                val wrapper = imageContainers.poll()

                // Dequeue the wrapper but update UI elements in the main thread
                withContext(Dispatchers.Main) {
                    // Fetch attributes from the wrapper
                    val imageView = wrapper.imageView
                    val btnLike = wrapper.btnLike
                    val btnDislike = wrapper.btnDislike
                    val btnSaved = wrapper.btnSaved
                    val captionTextView = wrapper.captionTextView
                    val containerLayoutId = wrapper.containerLayoutId
                    val containerTagsMap = wrapper.containerTagsMap
                    val serviceTag = containerTagsMap[containerLayoutId]?.service
                    val genreTags = containerTagsMap[containerLayoutId]?.genre
                    val likeState = containerTagsMap[containerLayoutId]?.likeState
                    val dislikeState = containerTagsMap[containerLayoutId]?.dislikeState
                    val savedState = containerTagsMap[containerLayoutId]?.savedState
                    val imageUrl = wrapper.imageUrl


                    if (imageView != null) {
                        Glide.with(imageView)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .centerCrop()
                            .placeholder(R.drawable.lotr) // Placeholder image while loading
                            .into(imageView)
                    }


                    // Update UI elements
                    if (captionTextView != null) {
                        captionTextView.text = wrapper.captionTextView?.text ?: ""

                    }

                    // Update like button state
                    if (btnLike != null) {
                        btnLike.isChecked = likeState ?: false
                    }
                    // Update dislike button visibility
                    if (btnDislike != null) {
                        btnDislike.isChecked = dislikeState ?: false
                    }
                    // Update saved button state
                    if (btnSaved != null) {
                        btnSaved.isChecked = savedState ?: false
                    }


                    // Print a message if captionTextView is null
                    if (captionTextView == null) {
                        Log.d("Caption", "CaptionTextView is null")
                    }
                }
            }
        }
    }


    private fun appendFormattedField(
        builder: StringBuilder,
        label: String,
        data: String,
        format: (String) -> String = { it }  // Default format does nothing
    ) {
        val formattedData = format(data)
        if (formattedData.isNotBlank()) {
            builder.append("$label $formattedData\n")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel() // Cancel the loop job when the activity is destroyed
    }


}
