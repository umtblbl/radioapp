package com.umit.radaioapp.mainActivity

import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.umit.radaioapp.model.Station

@Composable
fun PlayerBar(
    station: Station,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean
) {
    Surface(
        tonalElevation = 8.dp,
        color = Color.DarkGray,
        modifier = Modifier.height(90.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconSize = 60.dp
            val iconPaddingEnd = 24.dp

            if (!station.favicon.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(station.favicon)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Radyo Resmi",
                    modifier = Modifier
                        .size(iconSize)
                        .padding(end = iconPaddingEnd),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(Color.LightGray),
                    error = ColorPainter(Color.LightGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .padding(end = iconPaddingEnd)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Placeholder Radyo İkonu",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = station.name.orEmpty(),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            val buttonIconSize = 42.dp
            val buttonPadding = 6.dp

            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(buttonIconSize + buttonPadding * 2)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(buttonIconSize)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(buttonIconSize + buttonPadding * 2)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(buttonIconSize)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(buttonIconSize + buttonPadding * 2)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(buttonIconSize)
                )
            }

            val favoriteIconSize = 32.dp
            val favoriteButtonPadding = 4.dp

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(favoriteIconSize + favoriteButtonPadding * 2)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.Red,
                    modifier = Modifier.size(favoriteIconSize)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerBarPreview() {
    val sampleStation = Station(name = "My Radio Station")

    PlayerBar(
        station = sampleStation,
        isPlaying = true,
        onPlayPause = {},
        onNext = {},
        onPrevious = {},
        onToggleFavorite = {},
        isFavorite = true
    )
}
