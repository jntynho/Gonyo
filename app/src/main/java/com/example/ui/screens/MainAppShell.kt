package com.example.ui.screens

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ActiveVideoPlayback
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.ExoPlayerOverlay
import com.example.ui.components.GoPlayer
import com.example.ui.components.PhotosetLightbox
import com.example.ui.components.SmoothProgressIndicator
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import com.example.ui.theme.MotionTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(viewModel: MainViewModel) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val navState by viewModel.navState.collectAsStateWithLifecycle()
    val currentScreen = navState.currentScreen
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val activeLightbox by viewModel.activeLightbox.collectAsStateWithLifecycle()
    val resolvingStatus by viewModel.resolvingVideoStatus.collectAsStateWithLifecycle()
    val resolvingCardId by viewModel.resolvingCardId.collectAsStateWithLifecycle()
    val videoResolutionError by viewModel.videoResolutionError.collectAsStateWithLifecycle()
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()
    val transitionStyle = currentSettings.transitionStyle

    // Retain non-null overlay state so composing during exit animation does not collapse
    var lastActiveVideo by remember { mutableStateOf<ActiveVideoPlayback?>(null) }
    LaunchedEffect(activeVideo) {
        if (activeVideo != null) {
            lastActiveVideo = activeVideo
        }
    }

    var lastActiveLightbox by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    LaunchedEffect(activeLightbox) {
        if (activeLightbox != null) {
            lastActiveLightbox = activeLightbox
        }
    }

    // Smooth App Launch Entrance Animation
    var appEntranceVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appEntranceVisible = true
    }

    val canNavigateBack by viewModel.canNavigateBack.collectAsStateWithLifecycle()
    val isBackHandlingActive = activeLightbox != null || activeVideo != null || drawerState.isOpen || canNavigateBack

    // Predictive Back Gesture state
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackSwipeEdge by remember { mutableIntStateOf(0) }

    // Predictive Back Handling with clean priority chain
    PredictiveBackHandler(enabled = isBackHandlingActive) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
                predictiveBackSwipeEdge = backEvent.swipeEdge
            }
            predictiveBackProgress = 0f
            if (activeLightbox != null) {
                viewModel.closeLightbox()
            } else if (activeVideo != null) {
                viewModel.closeVideo()
            } else if (drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            } else {
                viewModel.navigateBack()
            }
        } catch (e: Exception) {
            predictiveBackProgress = 0f
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen is ScreenState.Home,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = palette.surface,
                drawerContentColor = palette.textPrimary,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accent,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Goony Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text("Goony", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = palette.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    val isHomeSelected = currentScreen is ScreenState.Home
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_home_solid),
                                contentDescription = "Home",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Home", fontWeight = if (isHomeSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isHomeSelected,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                viewModel.navigateTo(ScreenState.Home)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isActorsSelected = currentScreen is ScreenState.Actors || currentScreen is ScreenState.ActorScenes
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_actor_placeholder),
                                contentDescription = "Actor",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Actor", fontWeight = if (isActorsSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isActorsSelected,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                viewModel.navigateTo(ScreenState.Actors)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isStudiosSelected = currentScreen is ScreenState.Studios || currentScreen is ScreenState.StudioScenes
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_video_camera),
                                contentDescription = "Studio",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Studio", fontWeight = if (isStudiosSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isStudiosSelected,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                viewModel.navigateTo(ScreenState.Studios)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isBookmarksSelected = currentScreen is ScreenState.Bookmarks
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = if (isBookmarksSelected) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                                contentDescription = "Bookmark",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Bookmark", fontWeight = if (isBookmarksSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isBookmarksSelected,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                viewModel.navigateTo(ScreenState.Bookmarks)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val isStashDbSelected = currentScreen is ScreenState.StashDb
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_stashdb),
                                contentDescription = "StashDB",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("StashDb", fontWeight = if (isStashDbSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isStashDbSelected,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                viewModel.navigateTo(ScreenState.StashDb)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    val isSettingsSelected = currentScreen is ScreenState.Settings
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_solid),
                                contentDescription = "Settings & Sync",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Settings & Sync", fontWeight = if (isSettingsSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        selected = isSettingsSelected,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                viewModel.navigateTo(ScreenState.Settings)
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = accent.copy(alpha = 0.18f),
                            selectedTextColor = accent,
                            selectedIconColor = accent,
                            unselectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textSecondary
                        ),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        AnimatedVisibility(
            visible = appEntranceVisible,
            enter = fadeIn(animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.EasingEmphasizedDecelerate)),
            modifier = Modifier.fillMaxSize()
        ) {
            // Root solid background container to prevent any flicker through
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.bg)
            ) {
                // Predictive Back Interactive Transform Container
                val screenScale = 1f - (0.05f * predictiveBackProgress)
                val screenTransX = if (predictiveBackSwipeEdge == BackEventCompat.EDGE_RIGHT) {
                    -24f * predictiveBackProgress
                } else {
                    24f * predictiveBackProgress
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = screenScale
                            scaleY = screenScale
                            translationX = screenTransX
                        }
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val isBack = navState.direction == MainViewModel.NavigationDirection.BACK
                            val contentZIndex = if (isBack) 0f else 1f
                            when (transitionStyle) {
                                4 -> {
                                    // Option 4: Vertical Slide v2 (lighter fade-through vertical slide)
                                    (fadeIn(tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedDecelerate)) +
                                        slideInVertically(tween(MotionTokens.DurationLong, easing = MotionTokens.EasingStandard)) { h ->
                                            if (isBack) -h / MotionTokens.SlideFractionSubtleVertical else h / MotionTokens.SlideFractionSubtle
                                        })
                                        .togetherWith(
                                            fadeOut(tween(MotionTokens.DurationShort, easing = MotionTokens.EasingEmphasizedAccelerate)) +
                                            slideOutVertically(tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate)) { h ->
                                                if (isBack) h / MotionTokens.SlideFractionSubtle else -h / MotionTokens.SlideFractionSubtleVertical
                                            }
                                        )
                                        .apply { targetContentZIndex = contentZIndex }
                                }
                                1 -> {
                                    // Option 1: Fade-Through Slide (Default)
                                    if (isBack) {
                                        (fadeIn(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedDecelerate)) +
                                                slideInHorizontally(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingStandard)) { width -> -width / MotionTokens.SlideFractionHorizontal })
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(MotionTokens.DurationShort, easing = MotionTokens.EasingEmphasizedAccelerate)) +
                                                        slideOutHorizontally(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate)) { width -> width / MotionTokens.SlideFractionHorizontal }
                                            )
                                            .apply { targetContentZIndex = 0f }
                                    } else {
                                        (fadeIn(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedDecelerate)) +
                                                slideInHorizontally(animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.EasingStandard)) { width -> width / MotionTokens.SlideFractionHorizontal })
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(MotionTokens.DurationShort, easing = MotionTokens.EasingEmphasizedAccelerate)) +
                                                        slideOutHorizontally(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate)) { width -> -width / MotionTokens.SlideFractionHorizontal }
                                            )
                                            .apply { targetContentZIndex = 1f }
                                    }
                                }
                                else -> {
                                    // Option 0: Vertical Slide v2
                                    if (isBack) {
                                        (slideInVertically(animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.EasingStandard)) { fullHeight -> -fullHeight / MotionTokens.SlideFractionHorizontal } +
                                                fadeIn(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedDecelerate)))
                                            .togetherWith(
                                                slideOutVertically(animationSpec = tween(MotionTokens.DurationExtraLong, easing = MotionTokens.EasingStandard)) { fullHeight -> fullHeight / MotionTokens.SlideFractionVertical } +
                                                        fadeOut(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate))
                                            )
                                            .apply { targetContentZIndex = 0f }
                                    } else {
                                        (slideInVertically(animationSpec = tween(MotionTokens.DurationExtraLong, easing = MotionTokens.EasingStandard)) { fullHeight -> fullHeight / MotionTokens.SlideFractionVertical } +
                                                fadeIn(animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.EasingEmphasizedDecelerate)))
                                            .togetherWith(
                                                slideOutVertically(animationSpec = tween(MotionTokens.DurationLong, easing = MotionTokens.EasingStandard)) { fullHeight -> -fullHeight / MotionTokens.SlideFractionHorizontal } +
                                                        fadeOut(animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate))
                                            )
                                            .apply { targetContentZIndex = 1f }
                                    }
                                }
                            }
                        },
                        label = "screen_motion_transition"
                    ) { screen ->
                        when (screen) {
                            is ScreenState.Home -> HomeScreen(
                                viewModel = viewModel,
                                screen = screen,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.Bookmarks -> BookmarksScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.AddEditLink -> AddEditLinkScreen(viewModel, screen.linkId)
                            is ScreenState.Actors -> ActorManagementScreen(viewModel)
                            is ScreenState.ActorScenes -> HomeScreen(
                                viewModel = viewModel,
                                screen = screen,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.Studios -> StudioManagementScreen(viewModel)
                            is ScreenState.StudioScenes -> HomeScreen(
                                viewModel = viewModel,
                                screen = screen,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.PhotosetViewer -> PhotosetViewerScreen(viewModel, screen.title, screen.images, screen.initialIndex)
                            is ScreenState.StashDb -> StashDbScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                            )
                            is ScreenState.Settings -> SettingsScreen(viewModel)
                        }
                    }
                }

                // GoPlayer / ExoPlayer Video Player Overlay (Retains non-null instance during exit animation)
                AnimatedVisibility(
                    visible = activeVideo != null,
                    enter = fadeIn(tween(MotionTokens.DurationMedium)) + scaleIn(tween(MotionTokens.DurationMedium), initialScale = MotionTokens.ScaleOverlayInitial),
                    exit = fadeOut(tween(MotionTokens.DurationShort)) + scaleOut(tween(MotionTokens.DurationShort), targetScale = MotionTokens.ScaleOverlayInitial)
                ) {
                    lastActiveVideo?.let { video ->
                        GoPlayer(
                            title = video.title,
                            qualities = video.qualities,
                            subtitles = video.subtitles,
                            defaultHeaders = video.headers,
                            initialPositionMs = video.initialPositionMs,
                            startInLandscape = video.startInLandscape,
                            exoPlayer = viewModel.sharedPlayerManager.getPlayer(),
                            onClose = { viewModel.closeVideo() }
                        )
                    }
                }

                // High-Res Photoset Lightbox Overlay (Retains non-null instance during exit animation)
                AnimatedVisibility(
                    visible = activeLightbox != null,
                    enter = fadeIn(tween(MotionTokens.DurationMedium)) + scaleIn(tween(MotionTokens.DurationMedium), initialScale = MotionTokens.ScaleOverlayInitial),
                    exit = fadeOut(tween(MotionTokens.DurationShort)) + scaleOut(tween(MotionTokens.DurationShort), targetScale = MotionTokens.ScaleOverlayInitial)
                ) {
                    lastActiveLightbox?.let { (images, startIndex) ->
                        PhotosetLightbox(
                            images = images,
                            initialIndex = startIndex,
                            onClose = { viewModel.closeLightbox() }
                        )
                    }
                }

                // Video Resolving / Debrid Progress Overlay (Animated smooth fade in/out)
                AnimatedVisibility(
                    visible = resolvingCardId == null && resolvingStatus != null,
                    enter = fadeIn(tween(MotionTokens.DurationShort)),
                    exit = fadeOut(tween(MotionTokens.DurationShort))
                ) {
                    resolvingStatus?.let { statusText ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 320.dp)
                                    .padding(20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    SmoothProgressIndicator(
                                        modifier = Modifier.size(44.dp),
                                        color = accent,
                                        strokeWidth = 3.5.dp
                                    )
                                    Text(
                                        text = statusText,
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Video Resolution / Debrid Error Dialog
                if (resolvingCardId == null) {
                    videoResolutionError?.let { errText ->
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissVideoError() },
                            icon = {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "Stream Playback Error",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            },
                            text = {
                                Text(
                                    text = errText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = palette.textSecondary
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.dismissVideoError() },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                                ) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
