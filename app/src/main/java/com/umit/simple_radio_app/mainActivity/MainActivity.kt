package com.umit.simple_radio_app.mainActivity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.pager.*
import com.google.gson.Gson
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

    private val viewModel: RadioViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = RadioRepository(RetrofitInstance.api)
                val favManager = FavoritesManager(applicationContext)
                return RadioViewModel(repo, favManager) as T
            }
        }
    }

    private var currentPlayingStation by mutableStateOf<Station?>(null)
    private var loadingUrl by mutableStateOf<String?>(null)
    private var showDialog by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var searchQuery by mutableStateOf("")
    var stationToRemove by mutableStateOf<Station?>(null)
    private val gson = Gson()

    private var radioService: RadioService? = null
    private var isServiceBound = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RadioService.RadioServiceBinder
            radioService = binder.getService()
            isServiceBound.value = true
            radioService?.setPlaybackStatusCallback(playbackStatusCallback)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound.value = false
            radioService?.setPlaybackStatusCallback(null)
            radioService = null
        }
    }

    private val playbackStatusCallback = object : PlaybackStatusCallback {
        override fun onPlaybackStatusChanged(
            newIsPlaying: Boolean,
            newStation: Station?,
            newLoadingUrl: String?
        ) {
            isPlaying = newIsPlaying
            currentPlayingStation = newStation
            loadingUrl = newLoadingUrl
        }

        override fun onPlaybackError(errorMessage: String) {
            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            showDialog = "İnternet Bağlantınızı Kontrol Edin."
            isPlaying = false
            loadingUrl = null
            currentPlayingStation = null

            Log.e("MainActivityCallback", "onPlaybackError: $errorMessage")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, RadioService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        currentPlayingStation = viewModel.getLastPlayedUrl()

        setContent {
            val displayedStations by viewModel.displayedStations.collectAsState()
            val allStations by viewModel.allStations.collectAsState()
            val favorites by viewModel.favorites.collectAsState()
            val loading by viewModel.loading.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val context = LocalContext.current

            if (stationToRemove != null) {
                AlertDialog(
                    onDismissRequest = {
                        stationToRemove = null
                    },
                    title = { Text("Onay") },
                    text = { Text("Favorilerden kaldırmak istediğinize emin misiniz?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.toggleFavorite(stationToRemove!!)
                            stationToRemove = null
                        }) {
                            Text("Evet")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            stationToRemove = null
                        }) {
                            Text("Hayır")
                        }
                    }
                )
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
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 22.sp
                                        ),
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
                                            stationToRemove = station
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
                        visible = currentPlayingStation != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        currentPlayingStation?.let { station ->
                            PlayerBar(
                                station = station,
                                isPlaying = isPlaying,
                                onPlayPause = {
                                    val serviceIntent = Intent(context, RadioService::class.java)
                                    if (isPlaying) {
                                        serviceIntent.action = RadioService.ACTION_PAUSE
                                    } else {
                                        serviceIntent.action = RadioService.ACTION_PLAY
                                        serviceIntent.putExtra(
                                            RadioService.EXTRA_STATION_JSON,
                                            gson.toJson(station)
                                        )
                                    }
                                    context.startService(serviceIntent)
                                },
                                onToggleFavorite = {
                                    viewModel.toggleFavorite(station)
                                },
                                isFavorite = favorites.any { it.url == station.url },
                                onNext = {
                                    val currentIndex =
                                        allStations.indexOfFirst { it.url == currentPlayingStation?.url }
                                    if (currentIndex != -1) {
                                        val nextIndex = (currentIndex + 1) % allStations.size
                                        val nextStation = allStations[nextIndex]
                                        playStation(nextStation)
                                    }
                                },
                                onPrevious = {
                                    val currentIndex =
                                        allStations.indexOfFirst { it.url == currentPlayingStation?.url }
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

                    if (showDialog != null) {
                        AlertDialog(
                            onDismissRequest = { showDialog = null },
                            confirmButton = {
                                TextButton(onClick = { showDialog = null }) {
                                    Text("Tamam")
                                }
                            },
                            title = { Text("Hata") },
                            text = {
                                Text(
                                    text = if (showDialog?.isBlank() == false) showDialog.orEmpty()
                                    else "Radyo oynatılamadı, lütfen tekrar deneyin."
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun playStation(station: Station) {
        val serviceIntent = Intent(this, RadioService::class.java).apply {
            action = RadioService.ACTION_PLAY
            putExtra(RadioService.EXTRA_STATION_JSON, gson.toJson(station))
        }
        startService(serviceIntent)
        viewModel.saveLastPlayedUrl(station)
    }

    override fun onDestroy() {
        if (isServiceBound.value) {
            radioService?.setPlaybackStatusCallback(null)
            unbindService(serviceConnection)
            isServiceBound.value = false
        }
        val stopServiceIntent = Intent(this, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP
        }
        stopService(stopServiceIntent)
        super.onDestroy()
    }
}