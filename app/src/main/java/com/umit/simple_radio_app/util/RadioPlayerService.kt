package com.umit.simple_radio_app.util
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager

@UnstableApi
class RadioService : Service() {

    companion object {
        const val CHANNEL_ID = "radio_channel"
        const val NOTIFICATION_ID = 1234
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var playerNotificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // MediaSession oluştur (Player'ı bağla)
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        // Bildirim yöneticisi
        playerNotificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return "Radyo Oynatılıyor"
                }

                override fun createCurrentContentIntent(player: Player) : PendingIntent? {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    return PendingIntent.getActivity(this@RadioService, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return "Yayın devam ediyor"
                }

                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
                    return null
                }
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                @SuppressLint("ForegroundServiceType")
                override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(false)
                    }
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopSelf()
                }
            })
            .build()

        playerNotificationManager.setPlayer(player)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val streamUrl = intent?.getStringExtra("stream_url")

        if (streamUrl != null) {
            val mediaItem = MediaItem.fromUri(streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radyo Kanalı",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
