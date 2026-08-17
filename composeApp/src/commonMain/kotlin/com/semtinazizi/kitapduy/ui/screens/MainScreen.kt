package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.ui.components.MiniPlayerSection
import com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController
import org.koin.compose.koinInject

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val audioController = koinInject<GlobalAudioController>()
        val currentBook by audioController.currentBook.collectAsState()
        val isPlaying by audioController.isPlaying.collectAsState()
        val rootNavigator = LocalNavigator.currentOrThrow

        TabNavigator(HomeTab) {
            Scaffold(
                bottomBar = {
                    Column {
                        if (currentBook != null) {
                            MiniPlayerSection(
                                title = currentBook?.title ?: "",
                                artist = currentBook?.author ?: "",
                                coverUrl = currentBook?.coverUrl ?: "",
                                isPlaying = isPlaying,
                                isPremium = true, // Şimdilik varsayılan true
                                onClick = {
                                    currentBook?.let { book ->
                                        rootNavigator.push(com.semtinazizi.kitapduy.ui.screens.BookDetailScreen(book))
                                    }
                                },
                                onPlayPauseClick = {
                                    audioController.togglePlayPause()
                                },
                                onUpgradeClick = {
                                    rootNavigator.push(com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen())
                                }
                            )
                        }
                        NavigationBar {
                            TabNavigationItem(HomeTab)
                            TabNavigationItem(FeedTab)
                            TabNavigationItem(com.semtinazizi.kitapduy.ui.screens.tabs.EntertainmentTab)
                            TabNavigationItem(LibraryTab)
                            TabNavigationItem(ProfileTab)
                        }
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
    private fun RowScope.TabNavigationItem(tab: Tab) {
        val tabNavigator = LocalTabNavigator.current

        NavigationBarItem(
            selected = tabNavigator.current == tab,
            onClick = { tabNavigator.current = tab },
            icon = { 
                tab.options.icon?.let { 
                    Icon(painter = it, contentDescription = tab.options.title) 
                } 
            },
            label = { Text(tab.options.title) }
        )
    }
}
