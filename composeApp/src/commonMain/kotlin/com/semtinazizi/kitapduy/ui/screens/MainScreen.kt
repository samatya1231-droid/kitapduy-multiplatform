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

class MainScreen : Screen {
    @Composable
    override fun Content() {
        val audioController = koinInject<GlobalAudioController>()
        val currentBook by audioController.currentBook.collectAsState()
        val currentEpisode by audioController.currentEpisode.collectAsState()
        val isPlaying by audioController.isPlaying.collectAsState()
        val rootNavigator = LocalNavigator.currentOrThrow

        TabNavigator(HomeTab) {
            Scaffold(
                containerColor = SleekDarkBackground,
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
                                    rootNavigator.push(com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen())
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 0.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1C1C1E), // Koyu gri/siyah yüzey rengi (sabitlendi)
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                ),
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Üst ince altın çizgi
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, SleekChampagne.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernTabItem(HomeTab)
                        ModernTabItem(FeedTab)
                        ModernTabItem(EntertainmentTab)
                        
                        AskidaTabItem()
                        
                        ModernTabItem(LibraryTab)
                        ModernTabItem(ProfileTab)
                    }
                }
            }
        }
    }

    @Composable
    private fun ModernTabItem(tab: Tab, badgeCount: Int = 0) {
        val tabNavigator = LocalTabNavigator.current
        val isSelected = tabNavigator.current == tab
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.85f else if (isSelected) 1.15f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "scale"
        )

        val contentColor by animateColorAsState(
            targetValue = if (isSelected) SleekChampagne else Color.White.copy(alpha = 0.6f),
            label = "color"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .selectable(
                    selected = isSelected,
                    onClick = { tabNavigator.current = tab },
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = null
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .scale(scale)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text("$badgeCount", fontSize = 8.sp)
                        }
                    }
                }
            ) {
                tab.options.icon?.let { 
                    Icon(
                        painter = it, 
                        contentDescription = tab.options.title,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    ) 
                }
            }
            
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.options.title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    private fun AskidaTabItem() {
        val tabNavigator = LocalTabNavigator.current
        val isSelected = tabNavigator.current == AskidaTab
        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.15f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "askida_scale"
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { tabNavigator.current = AskidaTab }
                .padding(horizontal = 10.dp)
                .scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(SleekChampagne.copy(alpha = 0.3f), CircleShape)
                            .blur(12.dp)
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) SleekChampagne else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    border = BorderStroke(1.5.dp, SleekChampagne.copy(alpha = if (isSelected) 1f else 0.5f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.VolunteerActivism,
                            contentDescription = "Askıda",
                            tint = if (isSelected) Color.Black else SleekChampagne,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
