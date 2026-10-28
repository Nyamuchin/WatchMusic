package com.watchmusic

import android.app.Application

/**
 * Application entry point. The dependency container is created lazily so that the
 * media session service, which can be started before the activity, shares the same
 * preferences / artwork cache.
 */
class WatchMusicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(private val app: Application) {
    val prefs by lazy { com.watchmusic.data.PrefsStore(app) }
    val repository by lazy { com.watchmusic.data.MediaRepository(app, prefs) }
    val artwork by lazy { com.watchmusic.data.ArtworkLoader(app) }
    val volume by lazy { com.watchmusic.player.VolumeController(app) }
}
