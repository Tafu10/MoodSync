package com.example.moodsync.spotify

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SpotifyWebApiClient {

    // Maps moods to massive official Spotify Playlists
    private val playlistMap = mapOf(
        "Happy" to "37i9dQZF1DX3rxVfibe1L0",      // Mood Booster
        "Sad" to "37i9dQZF1DX3YSRoSdA634",        // Life Sucks
        "Surprise" to "37i9dQZF1DXcBWIGoYBM5M",   // Today's Top Hits
        "Angry" to "37i9dQZF1DWXRqgorJj26U",      // Rock Hard
        "Fear" to "37i9dQZF1DX4sWSpwq3LiO",       // Peaceful Piano (to calm down fear)
        "Disgust" to "37i9dQZF1DX4sWSpwq3LiO",    // Fallback
        "Neutral" to "37i9dQZF1DX4WYpdVIPcmO"     // Chill Hits
    )

    suspend fun getRandomTrackForMood(accessToken: String?, mood: String): Pair<String, String>? {
        if (accessToken == null) return null

        val playlistId = playlistMap[mood] ?: playlistMap["Neutral"]
        
        return withContext(Dispatchers.IO) {
            try {
                // Fetch the tracks from the playlist using the user's market to avoid geo-blocks
                val url = URL("https://api.spotify.com/v1/playlists/\$playlistId/tracks?limit=50&market=from_token")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer \$accessToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val reader = InputStreamReader(inputStream)
                    val responseStr = reader.readText()
                    reader.close()

                    val json = JSONObject(responseStr)
                    val items = json.getJSONArray("items")
                    
                    if (items.length() > 0) {
                        // Pick a random track from the 50 fetched
                        val randomIndex = (0 until items.length()).random()
                        val trackObj = items.getJSONObject(randomIndex).getJSONObject("track")
                        val trackName = trackObj.getString("name")
                        val trackUri = trackObj.getString("uri")
                        val artistsArray = trackObj.getJSONArray("artists")
                        val artistName = if (artistsArray.length() > 0) artistsArray.getJSONObject(0).getString("name") else ""
                        
                        val fullName = if (artistName.isNotEmpty()) "\$trackName by \$artistName" else trackName
                        return@withContext Pair(fullName, trackUri)
                    }
                } else {
                    Log.e("SpotifyWebAPI", "Failed to fetch playlist: \${connection.responseCode} - \${connection.responseMessage}")
                }
            } catch (e: Exception) {
                Log.e("SpotifyWebAPI", "Error fetching dynamic track: \${e.message}")
            }
            null
        }
    }
}
