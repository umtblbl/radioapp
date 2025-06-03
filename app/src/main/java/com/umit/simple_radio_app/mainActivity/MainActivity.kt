package com.umit.simple_radio_app.mainActivity

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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.accompanist.pager.*
import com.umit.simple_radio_app.api.RetrofitInstance
import com.umit.simple_radio_app.model.Station
import com.umit.simple_radio_app.repository.RadioRepository
import com.umit.simple_radio_app.util.FavoritesManager
import com.umit.simple_radio_app.viewmodel.RadioViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private enum class TabType {
        FAVORITES, ALL
    }

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
                    Player.STATE_READY, Player.STATE_ENDED, Player.STATE_IDLE -> loadingUrl = null
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
            val displayedStations by viewModel.displayedStations.collectAsState()
            val allStations by viewModel.allStations.collectAsState()
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

            val pagerState = rememberPagerState(initialPage = 0)
            val coroutineScope = rememberCoroutineScope()

            Scaffold(containerColor = Color.Black) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    val tabs = listOf(TabType.FAVORITES, TabType.ALL)

                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            ) {
                                Text(
                                    text = when (tab) {
                                        TabType.FAVORITES -> "Favoriler"
                                        TabType.ALL -> "Tüm Radyolar"
                                    },
                                    modifier = Modifier.padding(30.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
                                )
                            }
                        }
                    }

                    HorizontalPager(
                        count = tabs.size,
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.White)
                    ) { page ->
                        when (tabs[page]) {
                            TabType.ALL -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            searchQuery = it
                                            viewModel.onSearchQueryChanged(it)
                                        },
                                        label = {
                                            Text(
                                                text = "Radyo Ara",
                                                color = Color.White
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.DarkGray,
                                            unfocusedContainerColor = Color.DarkGray
                                        )
                                    )

                                    RadioGrid(
                                        stations = displayedStations,
                                        favorites = favorites.map { it.stationuuid.orEmpty() }
                                            .toSet(),
                                        onToggleFavorite = { station ->
                                            val stationToToggle =
                                                displayedStations.find { it.stationuuid == station.stationuuid }
                                            if (stationToToggle != null) {
                                                viewModel.toggleFavorite(stationToToggle)
                                            }
                                        },
                                        onPlay = { station ->
                                            playStation(station)
                                        },
                                        onEndReached = { viewModel.loadMore() },
                                        paginationTriggerIndex = displayedStations.size - 5,
                                        loadingUrl = loadingUrl
                                    )
                                }
                            }

                            TabType.FAVORITES -> {
                                val favoriteStations =
                                    favorites.toList()

                                if (favoriteStations.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Favori radyonuz yok",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 32.sp)
                                        )
                                    }
                                } else {
                                    RadioGrid(
                                        stations = favoriteStations,
                                        favorites = favorites.map { it.stationuuid.orEmpty() }
                                            .toSet(),
                                        onToggleFavorite = { station ->
                                            val stationToToggle =
                                                favoriteStations.find { it.stationuuid == station.stationuuid }
                                            if (stationToToggle != null) {
                                                viewModel.toggleFavorite(stationToToggle)
                                            }
                                        },
                                        onPlay = { station ->
                                            playStation(station)
                                        },
                                        onEndReached = { },
                                        paginationTriggerIndex = -1,
                                        loadingUrl = loadingUrl
                                    )
                                }
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
                                    viewModel.toggleFavorite(station)
                                },
                                isFavorite = favorites.any { it.url == station.url },
                                onNext = {
                                    val currentIndex =
                                        allStations.indexOfFirst { it.url == lastKnownStation?.url }
                                    if (currentIndex != -1) {
                                        val nextIndex = (currentIndex + 1) % allStations.size
                                        val nextStation = allStations[nextIndex]
                                        playStation(nextStation)
                                    }
                                },
                                onPrevious = {
                                    val currentIndex =
                                        allStations.indexOfFirst { it.url == lastKnownStation?.url }
                                    if (currentIndex != -1) {
                                        val prevIndex =
                                            if (currentIndex - 1 < 0) allStations.size - 1 else currentIndex - 1
                                        val prevStation = allStations[prevIndex]
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