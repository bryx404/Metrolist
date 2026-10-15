/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalNavController
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumViewTypeKey
import com.metrolist.music.constants.ChipSortTypeKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PlaylistViewTypeKey
import com.metrolist.music.utils.rememberEnumPreference

// Paleta de colores oficial estilo Spotify
private val SpotifyBlack = Color(0xFF000000)
private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyDarkGray = Color(0xFF2A2A2A)
private val SpotifyTextWhite = Color(0xFFFFFFFF)

@Composable
fun LibraryScreen() {
    val navController = LocalNavController.current
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    
    // Al configurar LIST como valor predeterminado, los cuadros de 'Descargado', 
    // 'En caché', etc. se convierten en filas de lista vertical compacta con miniatura.
    var libraryViewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.LIST)
    var playlistViewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.LIST)

    // Barra de filtros en cápsulas estilo Spotify
    val filterContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts),
            )

            filters.forEach { (type, label) ->
                val isSelected = filterType == type
                SpotifyFilterPill(
                    text = label,
                    isSelected = isSelected,
                    onClick = {
                        filterType = if (isSelected) LibraryFilter.LIBRARY else type
                    }
                )
            }
        }
    }

    // Forzamos el esquema de color oscuro puro estilo Spotify para toda la pantalla
    val spotifyTheme = darkColorScheme(
        background = SpotifyBlack,
        surface = SpotifyBlack,
        onBackground = SpotifyTextWhite,
        onSurface = SpotifyTextWhite
    )

    MaterialTheme(colorScheme = spotifyTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
        ) {
            when (filterType) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(
                    navController = navController,
                    filterContent = filterContent,
                    viewType = libraryViewType,
                    onViewTypeChange = { libraryViewType = it },
                )
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
                    navController = navController,
                    filterContent = filterContent,
                    viewType = playlistViewType,
                    onViewTypeChange = { playlistViewType = it },
                )
                LibraryFilter.SONGS -> LibrarySongsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY },
                )
                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY },
                )
                LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY },
                )
                LibraryFilter.PODCASTS -> LibraryPodcastsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY },
                )
            }
        }
    }
}

/**
 * Cápsula/Óvalo estilo Spotify: Gris oscuro con texto blanco (#2A2A2A)
 * que conmuta a Verde Spotify (#1DB954) al seleccionarse.
 */
@Composable
fun SpotifyFilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) SpotifyGreen else SpotifyDarkGray,
        label = "pill_background"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) SpotifyBlack else SpotifyTextWhite,
        label = "pill_text"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Contenedor de miniatura con indicador para videos de YouTube.
 * Si [isVideo] es true, añade un pequeño badge de reproductor en la esquina.
 */
@Composable
fun SpotifyMediaThumbnail(
    isVideo: Boolean,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.clip(shape)) {
        content()

        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = "Video",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}