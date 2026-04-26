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

    suspend fun getRandomTrackForMood(accessToken: String?, mood: String, onError: (String) -> Unit = {}): Pair<String, String>? {
        if (accessToken == null) {
            onError("No Access Token! Please click Link Spotify API.")
            return null
        }

        val playlistId = playlistMap[mood] ?: playlistMap["Neutral"]
        
        return withContext(Dispatchers.IO) {
            try {
                // Fetch up to 100 tracks from the playlist. 
                val url = URL("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=100")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val reader = InputStreamReader(inputStream)
                    val responseStr = reader.readText()
                    reader.close()

                    val json = JSONObject(responseStr)
                    val items = json.optJSONArray("items")
                    
                    if (items != null && items.length() > 0) {
                        val validTracks = mutableListOf<Pair<String, String>>()
                        
                        for (i in 0 until items.length()) {
                            val item = items.optJSONObject(i) ?: continue
                            val trackObj = item.optJSONObject("track") ?: continue
                            
                            val trackName = trackObj.optString("name", "")
                            val trackUri = trackObj.optString("uri", "")
                            if (trackUri.isEmpty() || trackName.isEmpty()) continue
                            
                            val artistsArray = trackObj.optJSONArray("artists")
                            val artistName = if (artistsArray != null && artistsArray.length() > 0) artistsArray.optJSONObject(0)?.optString("name", "") ?: "" else ""
                            
                            val fullName = if (artistName.isNotEmpty()) "$trackName by $artistName" else trackName
                            validTracks.add(Pair(fullName, trackUri))
                        }
                        
                        if (validTracks.isNotEmpty()) {
                            return@withContext validTracks.random()
                        } else {
                            onError("Playlist was empty or had no valid tracks.")
                        }
                    } else {
                        onError("No items returned from playlist.")
                    }
                } else {
                    val errorMsg = "HTTP ${connection.responseCode}: ${connection.responseMessage}"
                    Log.e("SpotifyWebAPI", errorMsg)
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Network Error: ${e.message}"
                Log.e("SpotifyWebAPI", errorMsg)
                onError(errorMsg)
            }
            null
        }
    }
}
