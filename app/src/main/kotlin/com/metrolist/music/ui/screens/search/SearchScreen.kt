/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalIsPlayerExpanded
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.MoodAndGenresViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ───────────────────────── Estilo Spotify ─────────────────────────
private val SpotifyBlack = Color(0xFF000000)
private val SearchBarWhite = Color(0xFFFFFFFF)
private val SearchBarText = Color(0xFF121212)
private val SearchBarHint = Color(0xFF5E5E5E)

/** Colores vibrantes de las tarjetas (rosa, verde oscuro, azul, morado, etc.) */
private val CardColors =
    listOf(
        Color(0xFFDC148C), // rosa
        Color(0xFF006450), // verde oscuro
        Color(0xFF2D46B9), // azul índigo
        Color(0xFF8400E7), // morado eléctrico
        Color(0xFF777777), // gris
        Color(0xFF8D67AB), // lavanda
        Color(0xFF1E3264), // azul marino
        Color(0xFFE8115B), // rojo-rosado
        Color(0xFF148A08), // verde
        Color(0xFF0D73EC), // azul vibrante
        Color(0xFFBA5D07), // naranja quemado
        Color(0xFF477D95), // azul acero
    )

@Composable
fun SearchScreen(
    pureBlack: Boolean, // se mantiene por compatibilidad: esta pantalla fuerza negro puro
    savedStateHandle: SavedStateHandle,
    moodAndGenresViewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lazyListState = rememberLazyListState()
    var isHandlingScrollToTop by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val moodAndGenres by moodAndGenresViewModel.moodAndGenres.collectAsStateWithLifecycle()

    val scrollToTopCount by savedStateHandle
        .getStateFlow("scrollToTopCount", 0)
        .collectAsStateWithLifecycle(initialValue = 0)

    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }

    // Al pulsar de nuevo la pestaña "Buscar" se enfoca la barra y se abre el teclado.
    // (Ya no se enfoca automáticamente al entrar: primero se ve la cuadrícula, como en Spotify.)
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            isHandlingScrollToTop = true

            kotlinx.coroutines.delay(100)

            if (!isPlayerExpanded) {
                focusManager.clearFocus(force = true)
                kotlinx.coroutines.delay(50)
                try {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (e: Exception) {
                }
            }

            kotlinx.coroutines.delay(500)
            isHandlingScrollToTop = false
        }
    }

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            return
        }

        focusManager.clearFocus()

        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                playerConnection?.playQueue(
                    YouTubeQueue(
                        WatchEndpoint(videoId = parsedUrl.id),
                    ),
                )
            }

            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                navController.navigate("online_playlist/${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Album -> {
                navController.navigate("album/MPREb_${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Artist -> {
                navController.navigate("artist/${parsedUrl.id}")
            }

            null -> {
                navController.navigate(SearchRoutes.resultRoute(searchQuery))
            }
        }

        if (!pauseSearchHistory) {
            coroutineScope.launch(Dispatchers.IO) {
                database.query {
                    insert(SearchHistory(query = searchQuery))
                }
            }
        }
    }

    val onSearch: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }
    val onSearchFromSuggestion: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }

    // La cuadrícula "Explorar todo" se ve cuando no se está escribiendo (modo online)
    val showExploreGrid =
        searchSource == SearchSource.ONLINE && query.text.isEmpty() && !isSearchFocused

    Scaffold(
        containerColor = SpotifyBlack,
    ) { _ ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(SpotifyBlack),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ───────── Barra de búsqueda blanca ─────────
                Row(
                    modifier =
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SearchBarWhite),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.dismiss),
                            tint = SearchBarText,
                        )
                    }

                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused },
                        textStyle =
                            TextStyle(
                                color = SearchBarText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        cursorBrush = SolidColor(SearchBarText),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (query.text.isEmpty()) {
                                Text(
                                    text =
                                        stringResource(
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            },
                                        ),
                                    style =
                                        TextStyle(
                                            color = SearchBarHint,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
                    )

                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { query = TextFieldValue("") }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                                tint = SearchBarText,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            searchSource =
                                if (searchSource == SearchSource.ONLINE) {
                                    SearchSource.LOCAL
                                } else {
                                    SearchSource.ONLINE
                                }
                        },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    when (searchSource) {
                                        SearchSource.LOCAL -> R.drawable.library_music
                                        SearchSource.ONLINE -> R.drawable.language
                                    },
                                ),
                            contentDescription = null,
                            tint = SearchBarText,
                        )
                    }
                }

                // ───────── Contenido ─────────
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showExploreGrid) {
                        ExploreAllGrid(
                            genres = moodAndGenres?.flatMap { it.items },
                            onGenreClick = { item ->
                                navController.navigate(
                                    "youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}",
                                )
                            },
                            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                            isLoading = moodAndGenres == null,
                        )
                    } else {
                        when (searchSource) {
                            SearchSource.LOCAL -> {
                                LocalSearchScreen(
                                    query = query.text,
                                    onDismiss = { navController.navigateUp() },
                                    pureBlack = true,
                                )
                            }

                            SearchSource.ONLINE -> {
                                OnlineSearchScreen(
                                    query = query.text,
                                    onQueryChange = { query = it },
                                    onSearch = onSearchFromSuggestion,
                                    onDismiss = { /* Don't dismiss when searching from suggestions */ },
                                    pureBlack = true,
                                )
                            }
                        }
                    }

                    HideOnScrollFAB(
                        lazyListState = lazyListState,
                        icon = R.drawable.mic,
                        onClick = { navController.navigate("recognition") },
                    )
                }
            }
        }
    }

    // Handle lifecycle events to manage keyboard visibility
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Always hide keyboard when resuming if player is expanded
                        if (isPlayerExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Clear focus when pausing to prevent keyboard from showing on resume
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial check - hide keyboard if player is expanded
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

// ───────────────────────── "Explorar todo" ─────────────────────────
@Composable
private fun ExploreAllGrid(
    genres: List<com.metrolist.innertube.pages.MoodAndGenres.Item>?,
    onGenreClick: (com.metrolist.innertube.pages.MoodAndGenres.Item) -> Unit,
    contentPadding: PaddingValues,
    isLoading: Boolean,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Explorar todo",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        if (genres == null && isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            }
        }

        genres?.let { list ->
            itemsIndexed(list, key = { index, item -> "${index}_${item.title}" }) { index, item ->
                GenreCard(
                    title = item.title,
                    color = CardColors[index % CardColors.size],
                    onClick = { onGenreClick(item) },
                )
            }
        }
    }
}

@Composable
private fun GenreCard(
    title: String,
    color: Color,
    onClick: () -> Unit,
) {
    val brush =
        Brush.linearGradient(
            colors = listOf(color, lerp(color, Color.White, 0.18f)),
        )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.75f)
                .clip(RoundedCornerShape(10.dp))
                .background(brush)
                .clickable(onClick = onClick)
                .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}