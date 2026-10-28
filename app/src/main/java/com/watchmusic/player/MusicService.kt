package com.watchmusic.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.watchmusic.MainActivity
import com.watchmusic.R

/**
 * Background playback host. Media3 owns the notification, the media session and
 * the foreground service lifecycle, so Bluetooth/lock-screen controls come for free.
 */
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        player = exoPlayer

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply { setSmallIcon(R.drawable.ic_notification_music) }
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player
        if (current == null || !current.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        player = null
        super.onDestroy()
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
    }
}
