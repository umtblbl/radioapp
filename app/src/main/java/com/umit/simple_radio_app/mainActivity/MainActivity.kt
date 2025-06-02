package com.umit.simple_radio_app.mainActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.umit.simple_radio_app.model.Station
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

    private var lastKnownStation: Station? = null
    private var currentPlayingUrl by mutableStateOf<String?>(null)

    private var loadingUrl by mutableStateOf<String?>(null)

    private var showDialog by mutableStateOf(false)
    private var isPlaying by mutableStateOf(false)
    private var selectedTabIndex by mutableStateOf(0)
    private var searchQuery by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build()

        lastKnownStation = viewModel.getLastPlayedUrl()
        currentPlayingUrl = lastKnownStation?.url

        lastKnownStation?.let { station ->
            val mediaItem = MediaItem.fromUri(Uri.parse(station.url))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> loadingUrl = currentPlayingUrl
                    Player.STATE_READY -> loadingUrl = null
                    Player.STATE_ENDED, Player.STATE_IDLE -> loadingUrl = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                loadingUrl = null
                showDialog = true
                exoPlayer.pause()
                isPlaying = false
            }
        })

        setContent {
            val stations by viewModel.stations.collectAsState()
            val favorites by viewModel.favorites.collectAsState()
            val loading by viewModel.loading.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val context = LocalContext.current

            DisposableEffect(Unit) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                }
                exoPlayer.addListener(listener)
                onDispose { exoPlayer.removeListener(listener) }
            }

            Scaffold(containerColor = Color.Black) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 }) {
                            Text("Favoriler", modifier = Modifier.padding(16.dp), color = Color.Black)
                        }
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 }) {
                            Text("Tüm Radyolar", modifier = Modifier.padding(16.dp), color = Color.Black)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        when (selectedTabIndex) {
                            1 -> {
                                Column {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        label = { Text("Radyo Ara") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    )

                                    val filteredStations = if (searchQuery.isBlank()) {
                                        stations
                                    } else {
                                        stations.filter {
                                            it.name?.contains(searchQuery, ignoreCase = true) == true
                                        }
                                    }

                                    RadioGrid(
                                        stations = filteredStations,
                                        favorites = favorites,
                                        onToggleFavorite = { url -> viewModel.toggleFavorite(url) },
                                        onPlay = { url ->
                                            val station = stations.find { it.url == url }
                                            if (station != null) {
                                                playStation(station)
                                            }
                                        },
                                        onEndReached = { viewModel.loadMore() },
                                        paginationTriggerIndex = stations.size - 5,
                                        loadingUrl = loadingUrl
                                    )
                                }
                            }

                            0 -> {
                                RadioGrid(
                                    stations = stations.filter { favorites.contains(it.url) },
                                    favorites = favorites,
                                    onToggleFavorite = { url -> viewModel.toggleFavorite(url) },
                                    onPlay = { url ->
                                        val station = stations.find { it.url == url }
                                        if (station != null) {
                                            playStation(station)
                                        }
                                    },
                                    onEndReached = { /* favoriler sayfasında sayfa sonu yok */ },
                                    paginationTriggerIndex = -1,
                                    loadingUrl = loadingUrl
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = currentPlayingUrl != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        lastKnownStation?.let { station ->
                            PlayerBar(
                                station = station,
                                isPlaying = isPlaying,
                                onPlayPause = {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                                onToggleFavorite = {
                                    station.url?.let {
                                        viewModel.toggleFavorite(it)
                                    }
                                },
                                isFavorite = favorites.contains(station.url),
                                onNext = {
                                    val currentIndex = stations.indexOfFirst { it.url == lastKnownStation?.url }
                                    if (currentIndex != -1) {
                                        val nextIndex = (currentIndex + 1) % stations.size
                                        val nextStation = stations[nextIndex]
                                        playStation(nextStation)
                                    }
                                },
                                onPrevious = {
                                    val currentIndex = stations.indexOfFirst { it.url == lastKnownStation?.url }
                                    if (currentIndex != -1) {
                                        val prevIndex = if (currentIndex - 1 < 0) stations.size - 1 else currentIndex - 1
                                        val prevStation = stations[prevIndex]
                                        playStation(prevStation)
                                    }
                                }
                            )
                        }
                    }

                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.Cyan)
                        )
                    }

                    if (errorMessage != null) {
                        LaunchedEffect(errorMessage) {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
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
                            title = { Text("Hata") },
                            text = { Text("Radyo oynatılamadı, lütfen tekrar deneyin.") }
                        )
                    }
                }
            }
        }
    }

    private fun playStation(station: Station) {
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(station.url))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            currentPlayingUrl = station.url
            lastKnownStation = station
            viewModel.saveLastPlayedUrl(station)
            loadingUrl = station.url
        } catch (e: Exception) {
            loadingUrl = null
            Toast.makeText(this, "Oynatma hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}
