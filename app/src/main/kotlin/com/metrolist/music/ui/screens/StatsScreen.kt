/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.StatPeriod
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalAlbumsGrid
import com.metrolist.music.ui.component.LocalArtistsGrid
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.LocalSongsGrid
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.TimeTransfer
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.StatsViewModel
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Constantes de estilo Spotify
private val SpotifyBlack = Color(0xFF000000)
private val SpotifyCardBg = Color(0xFF181818)
private val SpotifyCardHighlight = Color(0xFF222222)
private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyPillBg = Color(0xFF242424)
private val SpotifyTextWhite = Color(0xFFFFFFFF)
private val SpotifyTextMuted = Color(0xFFB3B3B3)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val sArtists = viewModel.selectedArtists

    val toggleArtistSelection: (Artist) -> Unit = { artist ->
        if (sArtists.any { it.id == artist.id }) {
            sArtists.removeAll { it.id == artist.id }
        } else {
            sArtists.add(artist)
        }
    }

    val clearArtistSelection: () -> Unit = {
        sArtists.clear()
    }
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<Long>, Long>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val indexChips by viewModel.indexChips.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val mostPlayedSongsStats by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val mostPlayedArtists by viewModel.filteredArtists.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.filteredAlbums.collectAsStateWithLifecycle()
    val allArtists by viewModel.mostPlayedArtists.collectAsStateWithLifecycle()
    val firstEvent by viewModel.firstEvent.collectAsStateWithLifecycle()
    val weeklyMostPlaylist by viewModel.weeklyMostPlaylist.collectAsStateWithLifecycle()
    val monthlyMostPlaylist by viewModel.monthlyMostPlaylist.collectAsStateWithLifecycle()
    val recapPlaylists by viewModel.recapPlaylists.collectAsStateWithLifecycle()
    val currentDate = LocalDateTime.now()
    val orderedMostPlayedSongs =
        remember(mostPlayedSongsStats, mostPlayedSongs) {
            val songsById = mostPlayedSongs.associateBy { it.song.id }
            mostPlayedSongsStats.mapNotNull { statsSong -> songsById[statsSong.id] }
        }
    val mostPeriodPlaylists = listOfNotNull(weeklyMostPlaylist, monthlyMostPlaylist)
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val visibleStatsPlaylists =
        remember(mostPeriodPlaylists, recapPlaylists, isLoggedIn) {
            if (isLoggedIn) {
                (mostPeriodPlaylists + recapPlaylists).distinctBy { it.id }
            } else {
                mostPeriodPlaylists
            }
        }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsStateWithLifecycle()

    var showTimeTransfer by rememberSaveable { mutableStateOf(false) }
    var prevOptionOrdinal by rememberSaveable { mutableStateOf<OptionStats?>(null) }
    var prevIndexChips by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(showTimeTransfer) {
        if (showTimeTransfer) {
            if (prevOptionOrdinal == null) prevOptionOrdinal = selectedOption
            if (prevIndexChips == null) prevIndexChips = indexChips
            viewModel.selectedOption.value = OptionStats.CONTINUOUS
            viewModel.indexChips.value = StatPeriod.ALL.ordinal
        }
    }

    if (showTimeTransfer) {
        TimeTransfer(
            onDismiss = {
                showTimeTransfer = false
                prevOptionOrdinal?.let { viewModel.selectedOption.value = it }
                prevIndexChips?.let { viewModel.indexChips.value = it }
                prevOptionOrdinal = null
                prevIndexChips = null
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.syncMostPlaylistsIfNeeded()
    }

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")
                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)
                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1),
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent?.event?.timestamp?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusYears(1).withDayOfYear(1).minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(firstEvent?.event?.timestamp)
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }

    // Estadísticas agregadas para el Bento Grid
    val totalSecondsListened = remember(mostPlayedSongsStats) {
        mostPlayedSongsStats.sumOf { it.timeListened ?: 0L }
    }
    val totalMinutes = remember(totalSecondsListened) {
        totalSecondsListened / 60
    }
    val formattedMinutes = remember(totalMinutes) {
        NumberFormat.getIntegerInstance(Locale.GERMANY).format(totalMinutes)
    }

    val topSong = mostPlayedSongsStats.firstOrNull()
    val topArtist = mostPlayedArtists.firstOrNull()
    val topAlbum = mostPlayedAlbums.firstOrNull()

    val currentPeriodLabel = remember(selectedOption, indexChips, weeklyDates, monthlyDates, yearlyDates) {
        when (selectedOption) {
            OptionStats.WEEKS -> weeklyDates.getOrNull(indexChips)?.second ?: "Esta semana"
            OptionStats.MONTHS -> monthlyDates.getOrNull(indexChips)?.second ?: "Este mes"
            OptionStats.YEARS -> yearlyDates.getOrNull(indexChips)?.second ?: "Este año"
            OptionStats.CONTINUOUS -> when (indexChips) {
                StatPeriod.WEEK_1.ordinal -> "Última semana"
                StatPeriod.MONTH_1.ordinal -> "Último mes"
                StatPeriod.MONTH_3.ordinal -> "Últimos 3 meses"
                StatPeriod.MONTH_6.ordinal -> "Últimos 6 meses"
                StatPeriod.YEAR_1.ordinal -> "Último año"
                else -> "Histórico completo"
            }
        }
    }

    val darkScheme = darkColorScheme(
        background = SpotifyBlack,
        surface = SpotifyBlack,
        onBackground = SpotifyTextWhite,
        onSurface = SpotifyTextWhite
    )

    MaterialTheme(colorScheme = darkScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
                modifier = Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                ),
            ) {
                val filteredArtists = allArtists
                    .map { artistWrapper ->
                        Artist(id = artistWrapper.artist.id, name = artistWrapper.artist.name)
                    }
                    .filter { artist -> artist.name.contains(query.text, ignoreCase = true) }

                // --- 1. FILTROS DE TIEMPO EN CÁPSULAS ESTILO SPOTIFY ---
                item(key = "spotify_choice_chips") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Modos principales: Continuo, Semanas, Meses, Años
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                OptionStats.CONTINUOUS to stringResource(R.string.continuous),
                                OptionStats.WEEKS to stringResource(R.string.weeks),
                                OptionStats.MONTHS to stringResource(R.string.months),
                                OptionStats.YEARS to stringResource(R.string.years),
                            )
                            options.forEach { (option, label) ->
                                SpotifyPill(
                                    text = label,
                                    isSelected = selectedOption == option,
                                    onClick = {
                                        viewModel.selectedOption.value = option
                                        viewModel.indexChips.value = 0
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sub-períodos específicos (1 semana, 1 mes, etc.)
                        val periodChips = when (selectedOption) {
                            OptionStats.WEEKS -> weeklyDates
                            OptionStats.MONTHS -> monthlyDates
                            OptionStats.YEARS -> yearlyDates
                            OptionStats.CONTINUOUS -> listOf(
                                StatPeriod.WEEK_1.ordinal to pluralStringResource(R.plurals.n_week, 1, 1),
                                StatPeriod.MONTH_1.ordinal to pluralStringResource(R.plurals.n_month, 1, 1),
                                StatPeriod.MONTH_3.ordinal to pluralStringResource(R.plurals.n_month, 3, 3),
                                StatPeriod.MONTH_6.ordinal to pluralStringResource(R.plurals.n_month, 6, 6),
                                StatPeriod.YEAR_1.ordinal to pluralStringResource(R.plurals.n_year, 1, 1),
                                StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                            )
                        }

                        if (periodChips.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                periodChips.forEach { (index, label) ->
                                    SpotifyPill(
                                        text = label,
                                        isSelected = indexChips == index,
                                        isSecondary = true,
                                        onClick = { viewModel.indexChips.value = index }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 2. BANNER DE RACHA (STREAK BANNER) ---
                if (!isSearching && topArtist != null) {
                    item(key = "streak_banner") {
                        val streakDays = minOf(topArtist.songCount, 7).coerceAtLeast(3)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(SpotifyCardHighlight, SpotifyCardBg)
                                    )
                                )
                                .clickable {
                                    navController.navigate("artist/${topArtist.id}")
                                }
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tuviste una racha de $streakDays días",
                                        color = SpotifyTextWhite,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Escuchaste a ${topArtist.artist.name} un día tras otro.",
                                        color = SpotifyTextMuted,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentPeriodLabel,
                                        color = SpotifyGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, SpotifyGreen, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = topArtist.artist.thumbnailUrl,
                                        contentDescription = topArtist.artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 3. MOSAICO ASIMÉTRICO (BENTO GRID ESTILO SPOTIFY WRAPPED) ---
                if (!isSearching && mostPlayedSongsStats.isNotEmpty()) {
                    item(key = "bento_grid_section") {
                        Text(
                            text = currentPeriodLabel,
                            color = SpotifyTextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // COLUMNA IZQUIERDA
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // TARJETA 1: MINUTOS DE ESCUCHA (NÚMEROS GIGANTES)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SpotifyCardBg)
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Minutos de escucha",
                                                color = SpotifyTextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = SpotifyTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = formattedMinutes,
                                            color = SpotifyTextWhite,
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1).sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "N.° 1 en tu biblioteca",
                                            color = SpotifyTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // TARJETA 2: ARTISTA TOP (RECTÁNGULO VERTICAL CON FOTO CIRCULAR)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SpotifyCardBg)
                                        .clickable {
                                            topArtist?.let { navController.navigate("artist/${it.id}") }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Artista top",
                                                color = SpotifyTextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = SpotifyTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = topArtist?.artist?.name ?: "—",
                                            color = SpotifyTextWhite,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "▲ 1",
                                                color = SpotifyGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Foto del artista en círculo grande
                                        Box(
                                            modifier = Modifier
                                                .size(118.dp)
                                                .clip(CircleShape)
                                                .background(SpotifyCardHighlight)
                                        ) {
                                            AsyncImage(
                                                model = topArtist?.artist?.thumbnailUrl,
                                                contentDescription = topArtist?.artist?.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            // COLUMNA DERECHA
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // TARJETA 3: CANCIÓN TOP
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SpotifyCardBg)
                                        .clickable {
                                            if (topSong != null) {
                                                if (topSong.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    val target = mostPlayedSongs.find { it.id == topSong.id }
                                                    if (target != null) {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                endpoint = WatchEndpoint(topSong.id),
                                                                preloadItem = target.toMediaMetadata(),
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Canción top",
                                                color = SpotifyTextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = SpotifyTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = topSong?.title ?: "—",
                                            color = SpotifyTextWhite,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(108.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SpotifyCardHighlight)
                                                .align(Alignment.CenterHorizontally)
                                        ) {
                                            AsyncImage(
                                                model = topSong?.thumbnailUrl,
                                                contentDescription = topSong?.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Indicador de reproducción si está activa
                                            if (topSong?.id == mediaMetadata?.id && isPlaying) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PlayArrow,
                                                        contentDescription = null,
                                                        tint = SpotifyGreen,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "${topSong?.songCountListened ?: 0} reproducciones",
                                            color = SpotifyTextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // TARJETA 4: ÁLBUM TOP / COMPLEMENTO
                                if (topAlbum != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(SpotifyCardBg)
                                            .clickable {
                                                navController.navigate("album/${topAlbum.id}")
                                            }
                                            .padding(16.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Álbum top",
                                                    color = SpotifyTextMuted,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = SpotifyTextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = topAlbum.album.title,
                                                color = SpotifyTextWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(86.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SpotifyCardHighlight)
                                                    .align(Alignment.CenterHorizontally)
                                            ) {
                                                AsyncImage(
                                                    model = topAlbum.album.thumbnailUrl,
                                                    contentDescription = topAlbum.album.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // --- 4. SECCIONES HORIZONTALES TRADICIONALES (RESTO DEL RANKING) ---
                if (visibleStatsPlaylists.isNotEmpty() && !isSearching && sArtists.isEmpty()) {
                    item(key = "mostPeriodPlaylistsTitle") {
                        NavigationTitle(
                            title = pluralStringResource(
                                R.plurals.n_playlist,
                                visibleStatsPlaylists.size,
                                visibleStatsPlaylists.size,
                            ),
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "mostPeriodPlaylists") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.animateItem(),
                        ) {
                            itemsIndexed(
                                items = visibleStatsPlaylists,
                                key = { _, playlist -> playlist.id },
                            ) { _, playlist ->
                                PlaylistGridItem(
                                    playlist = playlist,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("local_playlist/${playlist.id}")
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                if (!isSearching && mostPlayedSongsStats.isNotEmpty()) {
                    item(key = "mostPlayedSongs") {
                        NavigationTitle(
                            title = "${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs)}",
                            onPlayAllClick = if (orderedMostPlayedSongs.isNotEmpty()) {
                                {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.most_played_songs),
                                            items = orderedMostPlayedSongs.map { it.toMediaMetadata().toMediaItem() },
                                        ),
                                    )
                                }
                            } else null,
                            modifier = Modifier.animateItem(),
                        )

                        LazyRow(modifier = Modifier.animateItem()) {
                            itemsIndexed(
                                items = mostPlayedSongsStats,
                                key = { index, song -> "${song.id}_$index" },
                            ) { index, song ->
                                LocalSongsGrid(
                                    title = "${index + 1}. ${song.title}",
                                    subtitle = joinByBullet(
                                        pluralStringResource(
                                            R.plurals.n_time,
                                            song.songCountListened,
                                            song.songCountListened,
                                        ),
                                        makeTimeString(song.timeListened),
                                    ),
                                    thumbnailUrl = song.thumbnailUrl,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    val targetSong = mostPlayedSongs.find { it.id == song.id }
                                                    if (targetSong != null) {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                endpoint = WatchEndpoint(song.id),
                                                                preloadItem = targetSong.toMediaMetadata(),
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                val targetSong = mostPlayedSongs.find { it.id == song.id }
                                                if (targetSong != null) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = targetSong,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                if (!isSearching && mostPlayedArtists.isNotEmpty()) {
                    item(key = "mostPlayedArtists") {
                        NavigationTitle(
                            title = "${mostPlayedArtists.size} ${stringResource(id = R.string.artists)}",
                            modifier = Modifier.animateItem(),
                        )

                        LazyRow(modifier = Modifier.animateItem()) {
                            itemsIndexed(
                                items = mostPlayedArtists,
                                key = { _, artist -> artist.id },
                            ) { index, artist ->
                                LocalArtistsGrid(
                                    title = "${index + 1}. ${artist.artist.name}",
                                    subtitle = joinByBullet(
                                        pluralStringResource(
                                            R.plurals.n_time,
                                            artist.songCount,
                                            artist.songCount,
                                        ),
                                        makeTimeString(artist.timeListened?.toLong()),
                                    ),
                                    thumbnailUrl = artist.artist.thumbnailUrl,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("artist/${artist.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = artist,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                if (!isSearching && mostPlayedAlbums.isNotEmpty()) {
                    item(key = "mostPlayedAlbums") {
                        NavigationTitle(
                            title = "${mostPlayedAlbums.size} ${stringResource(id = R.string.albums)}",
                            modifier = Modifier.animateItem(),
                        )

                        LazyRow(modifier = Modifier.animateItem()) {
                            itemsIndexed(
                                items = mostPlayedAlbums,
                                key = { _, album -> album.id },
                            ) { index, album ->
                                LocalAlbumsGrid(
                                    title = "${index + 1}. ${album.album.title}",
                                    subtitle = joinByBullet(
                                        pluralStringResource(
                                            R.plurals.n_time,
                                            album.songCountListened ?: 0,
                                            album.songCountListened ?: 0,
                                        ),
                                        makeTimeString(album.timeListened),
                                    ),
                                    thumbnailUrl = album.album.thumbnailUrl,
                                    isActive = album.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${album.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = album,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }

                // --- 5. BÚSQUEDA ---
                if (isSearching) {
                    items(
                        items = allArtists.filter { artist ->
                            artist.artist.name.contains(query.text, ignoreCase = true)
                        },
                        key = { "stats_artist_${it.id}" },
                        contentType = { CONTENT_TYPE_ARTIST },
                    ) { artist ->
                        val uiArtist = Artist(name = artist.artist.name, id = artist.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleArtistSelection(uiArtist) }
                                .padding(8.dp),
                        ) {
                            ArtistListItem(
                                artist = artist,
                                modifier = Modifier.weight(1f),
                            )

                            Checkbox(
                                checked = sArtists.contains(Artist(name = artist.artist.name, id = artist.id)),
                                onCheckedChange = { toggleArtistSelection(uiArtist) },
                            )
                        }
                    }
                }

                if (query.text.isNotEmpty() && filteredArtists.isEmpty()) {
                    item(key = "no_result") {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }

            // Botón flotante para mezclar (Shuffle)
            if (mostPlayedSongsStats.isNotEmpty() && !isSearching) {
                HideOnScrollFAB(
                    visible = true,
                    lazyListState = lazyListState,
                    icon = R.drawable.shuffle,
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.most_played_songs),
                                items = orderedMostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled(),
                            ),
                        )
                    },
                )
            }
        }

        // --- BARRA SUPERIOR (TOP APP BAR ESTILO SPOTIFY) ---
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SpotifyBlack,
                titleContentColor = SpotifyTextWhite,
                navigationIconContentColor = SpotifyTextWhite,
                actionIconContentColor = SpotifyTextWhite,
            ),
            title = {
                if (inSelectMode) {
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                } else if (isSearching) {
                    Row {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SpotifyTextMuted
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = SpotifyTextWhite),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                        )

                        if (sArtists.isNotEmpty()) {
                            androidx.compose.material3.IconButton(onClick = clearArtistSelection) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Clear Artists",
                                    tint = SpotifyTextWhite,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Estadísticas de escucha",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            navigationIcon = {
                if (inSelectMode) {
                    androidx.compose.material3.IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Select Button",
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) {
                                navController.backToMain()
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back Button",
                            tint = SpotifyTextWhite
                        )
                    }
                }
            },
            actions = {
                if (inSelectMode) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = {},
                    )
                    androidx.compose.material3.IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {},
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "More Button",
                        )
                    }
                } else if (!isSearching) {
                    androidx.compose.material3.IconButton(
                        onClick = { isSearching = true },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search Button",
                            tint = SpotifyTextWhite
                        )
                    }
                    IconButton(
                        onClick = { showTimeTransfer = true },
                        onLongClick = { showTimeTransfer = true },
                    ) {
                        Icon(
                            painterResource(R.drawable.sync),
                            contentDescription = "Time Transfer",
                            tint = SpotifyTextWhite
                        )
                    }
                }
            },
        )
    }
}

/**
 * Cápsula/Óvalo estilo Spotify para filtros temporales:
 * Gris oscuro (#242424) que brilla en Verde Spotify (#1DB954) al seleccionarse.
 */
@Composable
fun SpotifyPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) SpotifyGreen else if (isSecondary) Color(0xFF1E1E1E) else SpotifyPillBg,
        animationSpec = tween(150),
        label = "pill_background"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else if (isSecondary) SpotifyTextMuted else SpotifyTextWhite,
        animationSpec = tween(150),
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

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }