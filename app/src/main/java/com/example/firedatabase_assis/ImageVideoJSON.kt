package com.example.firedatabase_assis

import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

fun parseResponse(jsonString: String): String? {
    var posterPath: String? = null

    try {
        // Parse the JSON string
        val jsonObject = JSONObject(jsonString)

        // Iterate through all fields in the JSON response
        for (key in jsonObject.keys()) {
            // Get the JSON array corresponding to the current field
            val resultsArray = jsonObject.optJSONArray(key)

            // If the field is an array, iterate through its elements
            resultsArray?.let {
                for (i in 0 until resultsArray.length()) {
                    // Get the JSON object at the current index
                    val resultObject = resultsArray.getJSONObject(i)

                    // Extract the poster path if it exists
                    if (resultObject.has("poster_path")) {
                        posterPath = resultObject.getString("poster_path")
                        // If there's only one poster path, return it immediately
                        return posterPath
                    }
                }
            }
        }
    } catch (e: JSONException) {
        e.printStackTrace()
        // Handle parsing error, maybe return null or throw an exception
    }

    return posterPath
}

fun extractStreamingService(text: String): String? {

    // Define regex pattern to extract streaming service
    val streamingServiceRegex = Pattern.compile("streamingService: (.*?)$", Pattern.MULTILINE)

    // Find streaming service using regex
    val streamingServiceMatcher = streamingServiceRegex.matcher(text)
    if (!streamingServiceMatcher.find()) {
        return null  // Return null if no streaming service found
    }

    // Extract streaming service name from the matched string

    return streamingServiceMatcher.group(1)?.trim()
}

fun extractGenres(text: String): List<String> {


    // Define regex pattern to extract genres
    val genreRegex = Pattern.compile("genres: \\[(.*?)\\]", Pattern.DOTALL)

    // Find genres using regex
    val genreMatcher = genreRegex.matcher(text)
    if (!genreMatcher.find()) {
        return emptyList()  // Return empty list if no genres found
    }

    // Extract genres from the matched string
    val genres = genreMatcher.group(1).split(", ").map { it.trim() }

    return genres
}

data class ContainerTags(
    val service: String?,
    val genre: List<String>?,
    var likeState: Boolean = false,
    var dislikeState: Boolean = false,
    var savedState: Boolean = false
)

