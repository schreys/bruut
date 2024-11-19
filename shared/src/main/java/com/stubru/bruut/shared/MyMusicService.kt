package com.stubru.bruut.shared

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 *
 *
 * To implement a MediaBrowserService, you need to:
 *
 *  *  Extend [MediaBrowserServiceCompat], implementing the media browsing
 * related methods [MediaBrowserServiceCompat.onGetRoot] and
 * [MediaBrowserServiceCompat.onLoadChildren];
 *
 *  *  In onCreate, start a new [MediaSessionCompat] and notify its parent
 * with the session"s token [MediaBrowserServiceCompat.setSessionToken];
 *
 *  *  Set a callback on the [MediaSessionCompat.setCallback].
 * The callback will receive all the user"s actions, like play, pause, etc;
 *
 *  *  Handle all the actual music playing using any method your app prefers (for example,
 * [android.media.MediaPlayer])
 *
 *  *  Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * [MediaSessionCompat.setPlaybackState]
 * [MediaSessionCompat.setMetadata] and
 * [MediaSessionCompat.setQueue])
 *
 *  *  Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 *
 * To make your app compatible with Android Auto, you also need to:
 *
 *  *  Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 *
 */

class MyMusicService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var focusRequest: AudioFocusRequest
    private lateinit var audioManager: AudioManager

    private var scrapingJob: Job? = null

    private val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            println("play")

            val result = audioManager.requestAudioFocus(focusRequest)

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                session.isActive = true
                session.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f).build())

                val metadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "VRT Studio Brussel")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Zware Gitaren")
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
                    .build()
                session.setMetadata(metadata)


                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setUri("http://icecast.vrtcdn.be/stubru_bruut-high.mp3")
                    .setLiveConfiguration(androidx.media3.common.MediaItem.LiveConfiguration.Builder().build())
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()

                //startScraping()
            }
        }

        override fun onSkipToQueueItem(queueId: Long) {
            println("onSkipToQueueItem")
        }

        override fun onSeekTo(position: Long) {
            println("onSeekTo")
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            println("onPlayFromMediaId")
            onPlay()
        }

        override fun onPause() {
            println("onPause")
            session.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f).build())
            exoPlayer.stop()
        }

        override fun onStop() {
            println("onStop")
            exoPlayer.stop()
            audioManager.abandonAudioFocusRequest(focusRequest)
            session.isActive = false
        }

        override fun onSkipToNext() {
            println("onSkipToNext")
        }

        override fun onSkipToPrevious() {
            println("onSkipToPrevious")
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            println("onCustomAction")
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            println("onPlayFromSearch")
        }
    }

    override fun onCreate() {
        super.onCreate()

        session = MediaSessionCompat(this, "MyMusicService")
        sessionToken = session.sessionToken
        session.setCallback(callback)

        stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        session.setPlaybackState(stateBuilder.build())

        exoPlayer = ExoPlayer.Builder(baseContext).build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> callback.onPause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> callback.onPause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> exoPlayer.volume = 0.2f
                    AudioManager.AUDIOFOCUS_GAIN -> exoPlayer.volume = 1.0f
                }
            }
            .build()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onDestroy() {
        session.release()
        //stopScraping()
        super.onDestroy()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        println(parentId)
        val mediaItems = mutableListOf<MediaItem>()

        if(parentId == "root")
        {
            val description = MediaDescriptionCompat.Builder()
                .setMediaId("0")
                .setTitle("VRT Studio Brussel")
                .build()

            val mediaItem = MediaItem(description, FLAG_BROWSABLE)
            mediaItems.add(mediaItem)
            result.sendResult(mediaItems)
            return
        }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId("1")
            .setTitle("Zware Gitaren")
            .build()

        val mediaItem = MediaItem(description, FLAG_PLAYABLE)
        mediaItems.add(mediaItem)

        result.sendResult(mediaItems)
    }


    object CurrentSong {
        var title: String? = null
        var artist: String? = null

        fun hasChanged(newTitle: String, newArtist: String): Boolean {
            return title != newTitle || artist != newArtist
        }

        fun update(newTitle: String, newArtist: String) {
            title = newTitle
            artist = newArtist
        }
    }

    private fun startScraping() {
        scrapingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val data = MetadataJsonExtractor.fetchNowPlaying()
                if (data != null) {
                    val (title, artist) = data
                    updateMediaMetadata(title, artist)
                    println("Updated Metadata: $title by $artist")
                }
                delay(5000) // Update every 5 seconds
            }
        }
    }

    private fun stopScraping() {
        scrapingJob?.cancel()
    }

    private fun updateMediaMetadata(title: String, artist: String) {
        if (CurrentSong.hasChanged(title, artist)) {
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .build()

            session.setMetadata(metadata)
            CurrentSong.update(title, artist)
            println("Metadata updated: $title by $artist")
        } else {
            println("Song has not changed. No update needed.")
        }
    }

}