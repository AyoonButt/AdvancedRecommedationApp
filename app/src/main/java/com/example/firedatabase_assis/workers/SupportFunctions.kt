import com.example.firedatabase_assis.database.Cast
import com.example.firedatabase_assis.database.Crew
import com.example.firedatabase_assis.database.UserSubscriptions
import com.example.firedatabase_assis.database.Users
import com.example.firedatabase_assis.workers.Video
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject

data class UserParams(
    val language: String,
    val region: String,
    val minMovie: Int,
    val maxMovie: Int,
    val minTV: Int,
    val maxTV: Int,
    val oldestDate: String,
    val recentDate: String
)

fun fetchUserParams(userId: Int): UserParams? {
    return transaction {
        Users.select { Users.userId eq userId }
            .mapNotNull {
                UserParams(
                    language = it[Users.language],
                    region = it[Users.region],
                    minMovie = it[Users.minMovie],
                    maxMovie = it[Users.maxMovie],
                    minTV = it[Users.minTV],
                    maxTV = it[Users.maxTV],
                    oldestDate = it[Users.oldestDate],
                    recentDate = it[Users.recentDate]
                )
            }
            .singleOrNull()
    }
}


fun getProvidersByPriority(userId: Int): List<Int> {
    return transaction {
        UserSubscriptions
            .select { UserSubscriptions.userId eq userId }
            .orderBy(UserSubscriptions.priority to SortOrder.ASC)
            .map { it[UserSubscriptions.providerID] }
    }
}

fun selectBestVideoKey(videos: List<Video>): String? {
    // Define the priority order for video types
    val priorityOrder = listOf("Short", "Trailer", "Teaser", "Featurette", "Clip")

    // Filter and sort videos based on priority and official status
    val filteredVideos = videos
        .filter { video ->
            video.isOfficial && priorityOrder.contains(video.type)
        }
        .sortedWith(
            compareBy(
                { priorityOrder.indexOf(it.type) },  // Prioritize by type
                { it.publishedAt }  // Then by publication date (newest first)
            )
        )

    // If no official videos found, look for unofficial trailers or any video published most recently
    val bestVideo = filteredVideos.lastOrNull()
        ?: videos
            .filter { video ->
                video.type == "Trailer" && !video.isOfficial
            }
            .maxByOrNull { it.publishedAt }
        ?: videos.maxByOrNull { it.publishedAt }

    return bestVideo?.key
}


fun parseAndInsertCredits(creditsJson: String, postId: Int) {
    // Convert JSON string to JSONObject
    val credits = JSONObject(creditsJson)

    // Parse and insert cast details
    val castArray = credits.getJSONArray("cast")
    for (i in 0 until castArray.length()) {
        val castMember = castArray.getJSONObject(i)
        val roles = castMember.getJSONArray("roles").getJSONObject(0)

        transaction {
            Cast.insert {
                it[Cast.postId] = postId
                it[personId] = castMember.getInt("id")
                it[name] = castMember.getString("name")
                it[gender] = castMember.optInt("gender", -1)
                it[knownForDepartment] = castMember.getString("known_for_department")
                it[character] = roles.optString("character", "")
                it[episodeCount] = roles.optInt("episode_count", 0)
                it[orderIndex] = castMember.optInt("order", -1)
                it[popularity] = castMember.optDouble("popularity", 0.0).toBigDecimal()
                it[profilePath] = castMember.optString("profile_path", null)
            }
        }
    }

    // Parse and insert crew details
    val crewArray = credits.getJSONArray("crew")
    for (i in 0 until crewArray.length()) {
        val crewMember = crewArray.getJSONObject(i)
        val jobs = crewMember.getJSONArray("jobs").getJSONObject(0)

        transaction {
            Crew.insert {
                it[Crew.postId] = postId
                it[personId] = crewMember.getInt("id")
                it[name] = crewMember.getString("name")
                it[gender] = crewMember.optInt("gender", -1)
                it[knownForDepartment] = crewMember.getString("known_for_department")
                it[job] = jobs.optString("job", "")
                it[department] = crewMember.optString("department", "")
                it[episodeCount] = jobs.optInt("episode_count", 0)
                it[popularity] = crewMember.optDouble("popularity", 0.0).toBigDecimal()
                it[profilePath] = crewMember.optString("profile_path", null)
            }
        }
    }
}

