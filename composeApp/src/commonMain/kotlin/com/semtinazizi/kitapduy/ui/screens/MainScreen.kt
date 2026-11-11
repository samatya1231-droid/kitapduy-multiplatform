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
import com.semtinazizi.kitapduy.ui.components.MiniPlayerSection
import com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne
import com.semtinazizi.kitapduy.ui.theme.SleekDarkBackground
import org.koin.compose.koinInject
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.util.root

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val audioController = koinInject<GlobalAudioController>()
        val currentBook by audioController.currentBook.collectAsState()
        val currentEpisode by audioController.currentEpisode.collectAsState()
        val isPlaying by audioController.isPlaying.collectAsState()
        val rootNavigator = LocalNavigator.currentOrThrow

        TabNavigator(HomeTab) { tabNavigator ->
            if (com.semtinazizi.kitapduy.platform.isSimulatorCiSmokeTest) {
                DisposableEffect(Unit) {
                    com.semtinazizi.kitapduy.platform.IosSmokeSimulationBridge.setMainScreenMounted(true)
                    onDispose {
                        com.semtinazizi.kitapduy.platform.IosSmokeSimulationBridge.setMainScreenMounted(false)
                    }
                }
                val targetTab by com.semtinazizi.kitapduy.platform.IosSmokeSimulationBridge.targetTab.collectAsState()

                LaunchedEffect(targetTab) {
                    targetTab?.let { tabNavigator.current = it }
                }
            }
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column(modifier = Modifier.background(Color.Transparent)) {
                        if (currentBook != null || currentEpisode != null) {
                            val title = currentBook?.title ?: currentEpisode?.title ?: ""
                            val artist = currentBook?.author ?: ""
                            val coverUrl = currentBook?.coverUrl ?: currentEpisode?.imageUrl ?: ""
                            
                            MiniPlayerSection(
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                isPlaying = isPlaying,
                                isPremium = true,
                                modifier = Modifier.padding(bottom = 8.dp),
                                onClick = {
                                    if (currentBook != null) {
                                        currentBook?.let { book ->
                                            rootNavigator.push(com.semtinazizi.kitapduy.ui.screens.BookDetailScreen(book))
                                        }
                                    } else if (currentEpisode != null) {
                                        currentEpisode?.let { episode ->
                                            rootNavigator.push(com.semtinazizi.kitapduy.ui.screens.PodcastDetailScreen(episode, artist))
                                        }
                                    }
                                },
                                onPlayPauseClick = {
                                    audioController.togglePlayPause()
                                },
                                onUpgradeClick = {
                                    rootNavigator.root.push(com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen())
                                }
                            )
                        }
                        
                        KitapDuyFloatingNavigation()
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    CurrentTab()
                }
            }
        }
    }

    @Composable
    private fun KitapDuyFloatingNavigation() {
        val tabNavigator = LocalTabNavigator.current
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            listOf(HomeTab, com.semtinazizi.kitapduy.ui.screens.tabs.SearchTab, FeedTab, LibraryTab, ProfileTab).forEach { tab ->
                NavigationBarItem(selected = tabNavigator.current == tab,
                    onClick = { tabNavigator.current = tab },
                    icon = { tab.options.icon?.let { Icon(painter = it, contentDescription = null) } },
                    label = { Text(tab.options.title, maxLines = 1) }, alwaysShowLabel = true)
            }
        }
    }
}
