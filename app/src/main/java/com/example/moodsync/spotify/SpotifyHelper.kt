package com.example.moodsync.spotify

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyHelper {

    private val clientId = "0d1515975b7543279b133bff92b6ca74"
    private val redirectUri = "com.example.moodsync://callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Track the last played emotion so we don't restart the song constantly
    private var lastPlayedEmotion: String? = null

    // Generate the Auth Request for the standalone Auth library
    fun getAuthRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            clientId,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        )
        .setShowDialog(false)
        .setScopes(arrayOf("app-remote-control", "user-read-playback-state", "user-modify-playback-state"))
        .build()
    }

    fun connect(context: Context, onConnected: () -> Unit, onTrackChanged: (String, android.graphics.Bitmap?) -> Unit, onFailure: (String) -> Unit) {
        // Disconnect any hanging or stale connections first
        SpotifyAppRemote.disconnect(spotifyAppRemote)
        spotifyAppRemote = null

        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(false) // Crucial: Disable built-in auth to prevent Android 15 IPC crash
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyHelper", "Connected to Spotify!")
                
                // Subscribe to PlayerState to get current track name and image
                appRemote.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
                    val track = playerState.track
                    if (track != null) {
                        val trackName = "${track.name} by ${track.artist.name}"
                        appRemote.imagesApi.getImage(track.imageUri).setResultCallback { bitmap ->
                            onTrackChanged(trackName, bitmap)
                        }.setErrorCallback {
                            onTrackChanged(trackName, null)
                        }
                    }
                }
                onConnected()
            }

            override fun onFailure(throwable: Throwable) {
                val errorMsg = throwable.message ?: "Unknown Error"
                Log.e("SpotifyHelper", "Spotify connection failed: $errorMsg")
                onFailure(errorMsg)
            }
        })
    }

    fun playPlaylistForEmotion(emotion: String?) {
        if (emotion == null || emotion == lastPlayedEmotion) return

        // A curated, randomized list of highly popular official Spotify playlists 
        // that are virtually guaranteed to exist and not be broken.
        val playlistUri = when (emotion) {
            "Happy" -> listOf(
                "spotify:playlist:37i9dQZF1DXdPec7aLTmlC", // Happy Hits
                "spotify:playlist:37i9dQZF1DX3rxVfibe1L0", // Mood Booster
                "spotify:playlist:37i9dQZF1DWSf2RDTDayIx"  // Happy Pop Hits
            ).random()
            "Sad" -> listOf(
                "spotify:playlist:37i9dQZF1DX3Ogo9pFvBkY", // Sad Covers
                "spotify:playlist:37i9dQZF1DX7qK8ma5wgG1", // Sad Songs
                "spotify:playlist:37i9dQZF1DWVV27DiNWxkR"  // Sad Indie
            ).random()
            "Surprise" -> listOf(
                "spotify:playlist:37i9dQZF1DX2L0iB23Enbq", // Viral Hits
                "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M", // Today's Top Hits
                "spotify:playlist:37i9dQZF1DX0b1hHYQtJso"  // Fresh Finds
            ).random()
            "Angry" -> listOf(
                "spotify:playlist:37i9dQZF1DX1tyCD9QhIWF", // Angry Workout
                "spotify:playlist:37i9dQZF1DWYiqJnxTcsru", // Walk Like A Badass
                "spotify:playlist:37i9dQZF1DX4eRPd9frC1m"  // Hype
            ).random()
            "Fear" -> listOf(
                "spotify:playlist:37i9dQZF1DX2pSTOxoPbx9", // Dark & Stormy
                "spotify:playlist:37i9dQZF1DX1s9knjP51Oa", // Tension
                "spotify:playlist:37i9dQZF1DWZAC1zOQZ0wG"  // Intense Studying
            ).random()
            "Disgust" -> listOf(
                "spotify:playlist:37i9dQZF1DX1lVhptIYRda", // Dirty Rock
                "spotify:playlist:37i9dQZF1DX1rVvRgjX59F", // Grunge Forever
                "spotify:playlist:37i9dQZF1DWZkhexvqF7Zc"  // Misfits
            ).random()
            else -> listOf(
                "spotify:playlist:37i9dQZF1DWZeKCadgRdKQ", // Deep Focus
                "spotify:playlist:37i9dQZF1DX8Uebhn9wzrS", // Chill Lofi Study Beats
                "spotify:playlist:37i9dQZF1DX4sWSpwq3LiO"  // Peaceful Piano
            ).random()
        }
        
        // Only play if connected
        spotifyAppRemote?.let {
            it.playerApi.play(playlistUri)
            lastPlayedEmotion = emotion
            Log.d("SpotifyHelper", "Playing playlist for emotion: $emotion")
        } ?: run {
            Log.w("SpotifyHelper", "Tried to play, but Spotify is not connected.")
        }
    }

    fun playUri(uri: String) {
        spotifyAppRemote?.let {
            it.playerApi.play(uri)
            Log.d("SpotifyHelper", "Playing specific URI: \$uri")
        } ?: run {
            Log.w("SpotifyHelper", "Tried to play URI, but Spotify is not connected.")
        }
    }

    fun skipNext() {
        spotifyAppRemote?.let {
            it.playerApi.skipNext()
            Log.d("SpotifyHelper", "Skipping to next track.")
        } ?: run {
            Log.w("SpotifyHelper", "Tried to skip, but Spotify is not connected.")
        }
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
            lastPlayedEmotion = null
        }
    }
}
