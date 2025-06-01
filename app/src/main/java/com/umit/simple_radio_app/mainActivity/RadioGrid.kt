package com.umit.simple_radio_app.mainActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.umit.simple_radio_app.model.Station

@Composable
fun RadioGrid(
    stations: List<Station>,
    favorites: Set<String>,
    paginationTriggerIndex: Int = stations.lastIndex,
    onToggleFavorite: (String) -> Unit,
    onPlay: (String) -> Unit,
    onEndReached: () -> Unit,
    loadingUrl: String? = null
) {
    val currentOnEndReached = rememberUpdatedState(onEndReached)

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        itemsIndexed(stations) { index, station ->

            val isLoading = loadingUrl == station.url

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.White)
                        .clickable(enabled = !isLoading) {
                            station.url?.let { onPlay(it) }
                        }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!station.favicon.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(station.favicon)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Radyo Resmi",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 16.dp),
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(Color.LightGray),
                            error = ColorPainter(Color.LightGray)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 16.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Placeholder Radyo İkonu",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = station.name ?: "Bilinmeyen Radyo",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp),
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 2.dp,
                            color = Color.Red
                        )
                    } else {
                        IconButton(
                            onClick = {
                                station.url?.let { onToggleFavorite(it) }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (favorites.contains(station.url)) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Favorilerden Kaldır",
                                    tint = Color.Red,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorilere Ekle",
                                    tint = Color.Red,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (index == paginationTriggerIndex) {
                LaunchedEffect(Unit) {
                    currentOnEndReached.value()
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun RadioGridPreview() {
    val sampleStations = listOf(
        Station(
            name = "Pop FM",
            url = "http://popfm.example.com/stream",
            favicon = "https://upload.wikimedia.org/wikipedia/commons/8/89/HD_transparent_picture.png"
        ),
        Station(
            name = "Rock Station",
            url = "http://rock.example.com/stream",
            favicon = null
        ),
        Station(
            name = "Jazz Vibes",
            url = "http://jazz.example.com/stream",
            favicon = ""
        )
    )
    RadioGrid(
        stations = sampleStations,
        favorites = setOf("http://popfm.example.com/stream"),
        onToggleFavorite = {},
        onPlay = {},
        onEndReached = {}
    )
}
