package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.semtinazizi.kitapduy.ui.screens.tabs.HomeTab
import com.semtinazizi.kitapduy.ui.screens.tabs.LibraryTab
import com.semtinazizi.kitapduy.ui.screens.tabs.EntertainmentTab
import com.semtinazizi.kitapduy.ui.screens.tabs.ProfileTab
import com.semtinazizi.kitapduy.ui.screens.tabs.AskidaTab
import com.semtinazizi.kitapduy.ui.screens.tabs.FeedTab
import com.semtinazizi.kitapduy.ui.screens.tabs.LocalTabRootRequest
import com.semtinazizi.kitapduy.ui.components.MiniPlayerSection
import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.platform.PushNotificationBridge
import com.semtinazizi.kitapduy.platform.PushNotificationRoute
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.theme.SleekDarkBackground
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val audioController = koinInject<GlobalAudioController>()
        val billingManager = koinInject<BillingManager>()
        val bookRepository = koinInject<BookRepository>()
        val authViewModel = koinViewModel<AuthViewModel>()
        val userProfile by authViewModel.userProfile.collectAsState()
        val isPremium = (billingManager.isPremium.collectAsState().value || userProfile?.hasPremium() == true)
        val currentBook by audioController.currentBook.collectAsState()
        val currentEpisode by audioController.currentEpisode.collectAsState()
        val currentPodcastTitle by audioController.currentPodcastTitle.collectAsState()
        val isFullPlayerVisible by audioController.isFullPlayerVisible.collectAsState()
        val isPlaying by audioController.isPlaying.collectAsState()
        val isBuffering by audioController.isBuffering.collectAsState()
        val currentPosition by audioController.currentPosition.collectAsState()
        val duration by audioController.duration.collectAsState()
        val pendingNotification by PushNotificationBridge.pendingNotification.collectAsState()
        val rootNavigator = LocalNavigator.currentOrThrow
        val tabRootRequests = remember { mutableStateMapOf<Tab, Int>() }

        LaunchedEffect(pendingNotification) {
            val event = pendingNotification ?: return@LaunchedEffect
            PushNotificationBridge.consumePendingNotification()
            when (val route = PushNotificationBridge.resolveRoute(event)) {
                is PushNotificationRoute.UserProfile -> {
                    rootNavigator.push(UserProfileScreen(route.userId))
                }
                is PushNotificationRoute.PostDetail -> {
                    rootNavigator.push(PostDetailScreen(route.postId))
                }
                is PushNotificationRoute.Podcast -> {
                    val resolved = runCatching {
                        bookRepository.resolvePodcastEpisode(route.podcastId, fallbackTitle = "Podcast")
                    }.getOrNull()
                    if (resolved != null) {
                        rootNavigator.push(PodcastDetailScreen(resolved.first, resolved.second))
                    } else {
                        rootNavigator.push(NotificationScreen())
                    }
                }
                is PushNotificationRoute.Book -> {
                    val book = runCatching {
                        bookRepository.getBookById(route.bookId)
                            ?: bookRepository.getBooks().find { it.id == route.bookId }
                    }.getOrNull()
                    if (book != null) {
                        rootNavigator.push(BookDetailScreen(book))
                    } else {
                        rootNavigator.push(NotificationScreen())
                    }
                }
                PushNotificationRoute.Notifications -> {
                    rootNavigator.push(NotificationScreen())
                }
            }
        }

        TabNavigator(HomeTab) { tabNavigator ->
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(modifier = Modifier.background(Color.Transparent)) {
                        if ((currentBook != null || currentEpisode != null) && !isFullPlayerVisible) {
                            val title = currentBook?.title ?: currentEpisode?.title ?: ""
                            val artist = currentBook?.author
                                ?: currentPodcastTitle?.takeIf { it.isNotBlank() }
                                ?: currentEpisode?.let { "Podcast" }
                                ?: ""
                            val coverUrl = currentBook?.coverUrl ?: currentEpisode?.imageUrl.orEmpty()
                            
                            MiniPlayerSection(
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                isPlaying = isPlaying,
                                isPremium = isPremium,
                                progress = if (duration > 0f) currentPosition else null,
                                isBuffering = isBuffering,
                                modifier = Modifier.padding(bottom = 8.dp),
                                onClick = {
                                    if (currentBook != null) {
                                        currentBook?.let { book ->
                                            rootNavigator.push(com.semtinazizi.kitapduy.ui.screens.BookDetailScreen(book))
                                        }
                                    } else if (currentEpisode != null) {
                                        currentEpisode?.let { episode ->
                                            rootNavigator.push(
                                                com.semtinazizi.kitapduy.ui.screens.PodcastDetailScreen(
                                                    episode,
                                                    currentPodcastTitle?.takeIf { it.isNotBlank() } ?: artist
                                                )
                                            )
                                        }
                                    }
                                },
                                onPlayPauseClick = {
                                    audioController.togglePlayPause()
                                },
                                onCloseClick = {
                                    audioController.stopPlayback()
                                },
                                onUpgradeClick = {
                                    rootNavigator.root.push(com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen())
                                }
                            )
                        }
                        
                        KitapDuyFloatingNavigation { tab ->
                            if (rootNavigator.canPop) {
                                val mainRoot = rootNavigator.items.firstOrNull { it is MainScreen }
                                    ?: rootNavigator.items.firstOrNull()
                                if (mainRoot != null) {
                                    rootNavigator.popUntil { it == mainRoot }
                                }
                            }
                            tabRootRequests[tab] = (tabRootRequests[tab] ?: 0) + 1
                            tabNavigator.current = tab
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    CompositionLocalProvider(
                        LocalTabRootRequest provides (tabRootRequests[tabNavigator.current] ?: 0)
                    ) {
                        CurrentTab()
                    }
                }
            }
        }
    }

    @Composable
    private fun KitapDuyFloatingNavigation(onSelect: (Tab) -> Unit) {
        val tabNavigator = LocalTabNavigator.current
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            listOf(
                "home" to HomeTab,
                "discover" to com.semtinazizi.kitapduy.ui.screens.tabs.SearchTab,
                "feed" to FeedTab,
                "library" to LibraryTab,
                "profile" to ProfileTab
            ).forEach { (name, tab) ->
                NavigationBarItem(selected = tabNavigator.current == tab,
                    modifier = Modifier.testTag("tab-$name"),
                    onClick = { onSelect(tab) },
                    icon = { tab.options.icon?.let { Icon(painter = it, contentDescription = null) } },
                    label = { Text(tab.options.title, maxLines = 1) }, alwaysShowLabel = true)
            }
        }
    }
}
