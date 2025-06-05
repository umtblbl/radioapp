package com.umit.simple_radio_app.mainActivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.gson.Gson
import com.umit.simple_radio_app.model.Station

interface PlaybackStatusCallback {
    fun onPlaybackStatusChanged(isPlaying: Boolean, currentStation: Station?, isLoading: String?)
    fun onPlaybackError(errorMessage: String)
}

class RadioService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    private var currentStation: Station? = null
    private val gson = Gson()
    private var callback: PlaybackStatusCallback? = null

    inner class RadioServiceBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    private val binder = RadioServiceBinder()

    companion object {
        const val ACTION_PLAY = "com.umit.simple_radio_app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.umit.simple_radio_app.ACTION_PAUSE"
        const val ACTION_STOP = "com.umit.simple_radio_app.ACTION_STOP"
        const val EXTRA_STATION_JSON = "extra_station_json"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "radio_channel"
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        currentStation?.let {
                            updateNotification(it, true)
                            // Buffer durumunda loadingUrl'i set et
                            callback?.onPlaybackStatusChanged(false, it, it.url)
                        }
                    }
                    Player.STATE_READY -> {
                        currentStation?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForeground(NOTIFICATION_ID, createNotification(it))
                            } else {
                                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, createNotification(it))
                            }
                            callback?.onPlaybackStatusChanged(true, it, null)
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            stopForeground(false)
                        } else {
                            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
                        }
                        callback?.onPlaybackStatusChanged(false, null, null)
                        stopSelf()
                    }
                    Player.STATE_IDLE -> {
                        if (!exoPlayer.isPlaying) {
                            callback?.onPlaybackStatusChanged(false, null, null)
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("RadioService", "Oynatıcı Hatası: ${error.errorCodeName}, Mesaj: ${error.message}, İstasyon: ${currentStation?.name}")
                callback?.onPlaybackError("Radyo oynatılamadı: ${error.message ?: "Bilinmeyen Hata"}")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopForeground(false)
                } else {
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
                }
                stopSelf()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                currentStation?.let {
                    updateNotification(it, false)
                }
                callback?.onPlaybackStatusChanged(isPlaying, currentStation, null)
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val stationJson = intent.getStringExtra(EXTRA_STATION_JSON)
                stationJson?.let {
                    val station = gson.fromJson(it, Station::class.java)
                    playStation(station)
                } ?: Log.e("RadioService", "Oynatılacak istasyon JSON'u boş.")
            }
            ACTION_PAUSE -> {
                exoPlayer.pause()
                currentStation?.let {
                    updateNotification(it, false)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    stopForeground(false)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playStation(station: Station) {
        currentStation = station
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(station.url))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            callback?.onPlaybackStatusChanged(false, station, station.url)
        } catch (e: Exception) {
            callback?.onPlaybackError("Oynatma başlatılamadı: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(false)
            }
            stopSelf()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Radyo Çalıyor",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(station: Station): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseAction = if (exoPlayer.isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPauseIcon = if (exoPlayer.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val playPausePendingIntent = PendingIntent.getService(
            this, 0, Intent(this, RadioService::class.java).apply {
                action = playPauseAction
                if (playPauseAction == ACTION_PLAY && currentStation != null) {
                    putExtra(EXTRA_STATION_JSON, gson.toJson(currentStation))
                }
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radyo Çalıyor")
            .setContentText(station.name)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(playPauseIcon, "Oynat/Duraklat", playPausePendingIntent)


        return builder.build()
    }

    private fun updateNotification(station: Station, isBuffering: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isBuffering) {
            val bufferingNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Radyo Yükleniyor...")
                .setContentText(station.name)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                .build()
            notificationManager.notify(NOTIFICATION_ID, bufferingNotification)
        } else {
            notificationManager.notify(NOTIFICATION_ID, createNotification(station))
        }
    }

    fun setPlaybackStatusCallback(callback: PlaybackStatusCallback?) {
        this.callback = callback
        val initialLoadingUrl = if (exoPlayer.playbackState == Player.STATE_BUFFERING) currentStation?.url else null
        callback?.onPlaybackStatusChanged(exoPlayer.isPlaying, currentStation, initialLoadingUrl)
    }

    override fun onDestroy() {
        exoPlayer.release()
        callback = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
}