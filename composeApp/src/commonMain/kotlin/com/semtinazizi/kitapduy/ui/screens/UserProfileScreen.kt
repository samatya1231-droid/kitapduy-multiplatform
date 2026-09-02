package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.model.FeedType
import com.semtinazizi.kitapduy.ui.components.PremiumBadge
import com.semtinazizi.kitapduy.ui.components.ProfileBentoStats
import com.semtinazizi.kitapduy.ui.components.ReadingDNACard
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.components.FeedCard
import com.semtinazizi.kitapduy.ui.viewmodel.ProfileScreenModel
import com.semtinazizi.kitapduy.ui.components.PremiumUpgradeCard
import com.semtinazizi.kitapduy.ui.screens.SubscriptionPaywallScreen

class UserProfileScreen(val targetUserId: String? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ProfileScreenModel>()
        
        val isOwnProfile = targetUserId == null
        val BrandGold = Color(0xFFFFD700)

        // Verileri load et
        LaunchedEffect(targetUserId) {
            screenModel.loadProfileData(targetUserId)
        }

        val userProfile by screenModel.targetUserProfile.collectAsState()
        val followersCount by screenModel.followersCount.collectAsState()
        val followingCount by screenModel.followingCount.collectAsState()
        val isFollowing by screenModel.isFollowing.collectAsState()
        val userShelves by screenModel.userShelves.collectAsState()
        val readingDna by screenModel.readingDnaCategories.collectAsState()
        val userPosts by screenModel.userPosts.collectAsState()
        val isPremium = userProfile?.hasPremium() ?: false

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(userProfile?.username?.let { "@$it" } ?: "Profil", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (!isOwnProfile) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                            }
                        }
                    },
                    actions = {
                        if (isOwnProfile) {
                            IconButton(onClick = { navigator.push(SettingsScreen(isPremium = userProfile?.hasPremium() == true)) }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ayarlar")
                            }
                            IconButton(onClick = { navigator.push(ProfileEditScreen()) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Düzenle")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Background Blobs
                val infiniteTransition = rememberInfiniteTransition(label = "profile_bg_blobs")
                val blobOffset1 by infiniteTransition.animateValue(
                    initialValue = IntOffset(100, 100),
                    targetValue = IntOffset(-100, -100),
                    typeConverter = IntOffset.VectorConverter,
                    animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                    label = "offset1"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(blobOffset1.x.dp, blobOffset1.y.dp)
                        .size(350.dp)
                        .background(BrandGold.copy(alpha = 0.08f), CircleShape)
                        .blur(100.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        // Kapak & Profil Fotoğrafı Section
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                            // Cover Photo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (userProfile?.coverPhotoUrl != null) {
                                    SmartImage(
                                        imageUrl = userProfile?.coverPhotoUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(BrandGold.copy(alpha = 0.2f)))
                                }
                            }
                            
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 10.dp)
                            ) {
                                SmartImage(
                                    imageUrl = userProfile?.photoUrl,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .border(if (isPremium) 6.dp else 4.dp, if (isPremium) BrandGold else Color.Gray.copy(0.3f), CircleShape),
                                    contentScale = ContentScale.Crop,
                                    isAvatar = true,
                                    fallbackText = userProfile?.name ?: ""
                                )
                                if (isPremium) {
                                    PremiumBadge(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // İsim ve Bio
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = userProfile?.name ?: "Kullanıcı",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = userProfile?.bio ?: "Henüz bir biyografi yazılmamış.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            if (!isOwnProfile && targetUserId != null) {
                                Button(
                                    onClick = { screenModel.toggleFollow(targetUserId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else BrandGold,
                                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isFollowing) "Takipten Çık" else "Takip Et", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // İstatistikler (Bento)
                        ProfileBentoStats(
                            followersCount = followersCount.toString(),
                            followingCount = followingCount.toString(),
                            booksFinished = userShelves.size.toString(),
                            streakDays = (userProfile?.streak ?: 0).toString(),
                            onFollowersClick = { /* Go to Follow List */ },
                            onFollowingClick = { /* Go to Follow List */ },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Okuma DNA'sı
                        ReadingDNACard(
                            categories = readingDna,
                            title = "Okuma DNA'sı",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        if (isOwnProfile) {
                            PremiumUpgradeCard(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                                isPremium = isPremium,
                                onClick = { navigator.push(SubscriptionPaywallScreen()) }
                            )
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        Text(
                            "Paylaşımlar",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (userPosts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Henüz paylaşım yok.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(userPosts) { post ->
                            FeedCard(
                                item = post,
                                currentUserId = targetUserId,
                                onClick = {},
                                onUserClick = {},
                                onImageClick = {},
                                onDeletePost = {},
                                onToggleLike = { _, _, _ -> },
                                onVotePoll = { _, _ -> },
                                onPlayAudio = { _, _ -> },
                                onPauseAudio = {},
                                isAudioPlaying = false,
                                isAudioLoading = false,
                                audioProgress = 0f
                            )
                        }
                    }
                }
            }
        }
    }
}
