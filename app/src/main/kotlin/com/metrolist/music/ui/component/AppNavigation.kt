/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// ───────────────────────── Paleta estilo Spotify ─────────────────────────
private val SpotifyBlack = Color(0xFF000000)
private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyInactive = Color(0xFFB3B3B3)

private val NavIconSize = 26.dp
private const val HOME_HOLD_MILLIS = 15_000L

@Stable
private fun isRouteSelected(
    currentRoute: String?,
    screenRoute: String,
    navigationItems: List<Screens>,
): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")
    ) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")
    ) return true

    return false
}

/**
 * Detecta pulsación larga mediante el InteractionSource (Home: mantener 15 s,
 * Search: long press normal). Si no es long press, dispara el click normal al soltar.
 */
@Composable
private fun LongPressHandler(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    holdMillis: Long,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onShortClick: () -> Unit,
) {
    if (!enabled) return

    val haptics = LocalHapticFeedback.current
    val currentIsSelected by rememberUpdatedState(isSelected)
    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentShortClick by rememberUpdatedState(onShortClick)

    LaunchedEffect(interactionSource, holdMillis) {
        var isLongClick = false
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(holdMillis)
                    isLongClick = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentLongPress()
                }

                is PressInteraction.Release -> {
                    if (!isLongClick) currentShortClick()
                }

                is PressInteraction.Cancel -> {
                    isLongClick = false
                }
            }
        }
    }
}

// ───────────────────────── Rail (tablets / horizontal) ─────────────────────────
@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = true,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
    onCreateClick: (() -> Unit)? = null,
) {
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = SpotifyBlack,
        contentColor = SpotifyInactive,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = if (isSelected) screen.iconIdActive else screen.iconIdInactive

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            LongPressHandler(
                interactionSource = interactionSource,
                enabled = isSearchItem || isHomeHoldItem,
                holdMillis = if (isHomeHoldItem) HOME_HOLD_MILLIS else viewConfiguration.longPressTimeoutMillis,
                isSelected = isSelected,
                onLongPress = {
                    if (isHomeHoldItem) onHomeLongHold?.invoke() else onSearchLongClick?.invoke()
                },
                onShortClick = { onItemClick(screen, currentIsSelected) },
            )

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Las pulsaciones largas se gestionan vía InteractionSource
                },
                interactionSource = interactionSource,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = SpotifyGreen,
                    selectedTextColor = SpotifyGreen,
                    unselectedIconColor = SpotifyInactive,
                    unselectedTextColor = SpotifyInactive,
                    indicatorColor = Color.Transparent,
                ),
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId),
                        modifier = Modifier.size(NavIconSize),
                    )
                },
            )
        }

        if (onCreateClick != null) {
            NavigationRailItem(
                selected = false,
                onClick = onCreateClick,
                colors = NavigationRailItemDefaults.colors(
                    unselectedIconColor = SpotifyInactive,
                    indicatorColor = Color.Transparent,
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Crear",
                        modifier = Modifier.size(NavIconSize + 4.dp),
                    )
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ───────────────────────── Barra inferior estilo Spotify ─────────────────────────
@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = true,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
    /** Botón "+" del extremo derecho (crear lista / sala). Si es null no se muestra. */
    onCreateClick: (() -> Unit)? = null,
    createLabel: String = "Crear",
) {
    val viewConfiguration = LocalViewConfiguration.current

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = SpotifyGreen,
        selectedTextColor = SpotifyGreen,
        unselectedIconColor = SpotifyInactive,
        unselectedTextColor = SpotifyInactive,
        indicatorColor = Color.Transparent, // sin "píldora": solo brilla el verde
    )

    NavigationBar(
        modifier = modifier,
        containerColor = SpotifyBlack,
        contentColor = SpotifyInactive,
        tonalElevation = 0.dp,
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = if (isSelected) screen.iconIdActive else screen.iconIdInactive

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            LongPressHandler(
                interactionSource = interactionSource,
                enabled = isSearchItem || isHomeHoldItem,
                holdMillis = if (isHomeHoldItem) HOME_HOLD_MILLIS else viewConfiguration.longPressTimeoutMillis,
                isSelected = isSelected,
                onLongPress = {
                    if (isHomeHoldItem) onHomeLongHold?.invoke() else onSearchLongClick?.invoke()
                },
                onShortClick = { onItemClick(screen, currentIsSelected) },
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Las pulsaciones largas se gestionan vía InteractionSource
                },
                interactionSource = interactionSource,
                colors = itemColors,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId),
                        modifier = Modifier.size(NavIconSize),
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else null,
                alwaysShowLabel = !slimNav,
            )
        }

        // Botón "+" fijo en el extremo derecho
        if (onCreateClick != null) {
            NavigationBarItem(
                selected = false,
                onClick = onCreateClick,
                colors = itemColors,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = createLabel,
                        modifier = Modifier.size(NavIconSize + 4.dp),
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = createLabel,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else null,
                alwaysShowLabel = !slimNav,
            )
        }
    }
}