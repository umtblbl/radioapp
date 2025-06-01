package com.umit.simple_radio_app.mainActivity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.umit.simple_radio_app.api.RetrofitInstance
import com.umit.simple_radio_app.repository.RadioRepository
import com.umit.simple_radio_app.util.FavoritesManager
import com.umit.simple_radio_app.viewmodel.RadioViewModel

class MainActivity : ComponentActivity() {

    private lateinit var exoPlayer: ExoPlayer

    private val viewModel: RadioViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = RadioRepository(RetrofitInstance.api)
                val favManager = FavoritesManager(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return RadioViewModel(repo, favManager) as T
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                val stations by viewModel.stations.collectAsState()
                val favorites by viewModel.favorites.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()

                val context = LocalContext.current

                // Şu an çalan URL
                var currentPlayingUrl by remember { mutableStateOf<String?>(null) }
                // Yükleniyor gösterimi için url
                var loadingUrl by remember { mutableStateOf<String?>(null) }
                var showDialog by remember { mutableStateOf(false) }

                var selectedTabIndex by remember { mutableStateOf(0) }

                // ExoPlayer Listener Compose scope içinde
                DisposableEffect(Unit) {
                    val listener = object : Player.Listener {

                        override fun onPlaybackStateChanged(state: Int) {
                            when(state) {
                                Player.STATE_BUFFERING -> {
                                    // Buffering başladı
                                    loadingUrl = currentPlayingUrl
                                }
                                Player.STATE_READY -> {
                                    // Oynatma hazır
                                    loadingUrl = null
                                }
                                Player.STATE_ENDED, Player.STATE_IDLE -> {
                                    loadingUrl = null
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            loadingUrl = null
                            showDialog = true
                            currentPlayingUrl = null
                        }
                    }
                    exoPlayer.addListener(listener)

                    onDispose {
                        exoPlayer.removeListener(listener)
                    }
                }

                Scaffold(
                    containerColor = Color(0xFFF5F5F5)
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(Color.LightGray)
                    ) {
                        TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color(0xFFF5F5F5)) {
                            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                                Text("Tüm Radyolar", modifier = Modifier.padding(16.dp), color = Color.Black)
                            }
                            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                                Text("Favoriler", modifier = Modifier.padding(16.dp), color = Color.Black)
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedTabIndex) {
                                0 -> {
                                    RadioGrid(
                                        stations = stations,
                                        favorites = favorites,
                                        onToggleFavorite = { url -> viewModel.toggleFavorite(url) },
                                        onPlay = { url ->
                                            loadingUrl = url
                                            currentPlayingUrl = url
                                            val mediaItem = MediaItem.fromUri(Uri.parse(url))
                                            exoPlayer.setMediaItem(mediaItem)
                                            exoPlayer.prepare()
                                            exoPlayer.play()
                                        },
                                        onEndReached = { viewModel.loadMore() },
                                        paginationTriggerIndex = stations.size - 5,
                                        loadingUrl = loadingUrl
                                    )
                                }
                                1 -> {
                                    RadioGrid(
                                        stations = stations.filter { favorites.contains(it.url) },
                                        favorites = favorites,
                                        onToggleFavorite = { url -> viewModel.toggleFavorite(url) },
                                        onPlay = { url ->
                                            loadingUrl = url
                                            currentPlayingUrl = url
                                            val mediaItem = MediaItem.fromUri(Uri.parse(url))
                                            exoPlayer.setMediaItem(mediaItem)
                                            exoPlayer.prepare()
                                            exoPlayer.play()
                                        },
                                        onEndReached = {},
                                        paginationTriggerIndex = Int.MAX_VALUE,
                                        loadingUrl = loadingUrl
                                    )
                                }
                            }
                        }
                    }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            confirmButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Tamam")
                                }
                            },
                            title = { Text("Radyo Hatası") },
                            text = { Text("Bu radyo şu an kullanılamıyor, lütfen başka radyo seçiniz.") }
                        )
                    }

                    if (errorMessage != null) {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}
