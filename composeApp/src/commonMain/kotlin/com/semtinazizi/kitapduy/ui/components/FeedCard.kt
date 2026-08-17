package com.semtinazizi.kitapduy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.FeedType

@Composable
fun SmartImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fallbackText: String = "",
    isAvatar: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = fallbackText,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.background(Color.Gray), contentAlignment = Alignment.Center) {
            Text(if (fallbackText.isNotEmpty()) fallbackText.take(1).uppercase() else "?", color = Color.White)
        }
    }
}

@Composable
fun PremiumBadge() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color(0xFFFFD700), CircleShape)
            .padding(2.dp)
    ) {
        Text("👑", fontSize = 8.sp, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun LikeExplosion(
    trigger: Boolean,
    color: Color,
    onAnimationEnd: () -> Unit
) {
    // Basic explosion effect placeholder
}

@Composable
fun WaveformProgressBar(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Yellow
) {
    Box(
        modifier = modifier
            .background(Color.DarkGray, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(4.dp))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedCard(
    item: FeedItem,
    currentUserId: String?,
    isAdmin: Boolean = false,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onToggleLike: (String, String, Int) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onPlayAudio: (String, Long?) -> Unit,
    onPauseAudio: () -> Unit,
    isAudioPlaying: Boolean,
    isAudioLoading: Boolean,
    audioProgress: Float,
    isFullWidth: Boolean = true,
    showCommentSection: Boolean = true
) {
    var showMenu by remember(item.id) { mutableStateOf(false) }
    val isMyPost = currentUserId == item.userId
    val canDeletePost = isMyPost || isAdmin
    val isPremiumUserPost = item.isPremium || item.badges.contains("premium") || item.userName == "Dünyazede"
    
    val safeName = if (item.userName.isNotBlank()) item.userName else "Anonim"
    val haptic = LocalHapticFeedback.current

    var isLikedLocally by remember(item.id, item.likedBy) {
        mutableStateOf(item.likedBy.any { it.startsWith(currentUserId ?: "") })
    }
    var likeCountLocally by remember(item.id, item.likedBy) { mutableIntStateOf(item.likedBy.size) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(if (isFullWidth) 24.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isPremiumUserPost) BorderStroke(1.dp, Color(0xFFFFD700)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onUserClick() }.padding(2.dp)
                ) {
                    SmartImage(
                        imageUrl = item.userPhotoUrl,
                        modifier = Modifier.size(34.dp).clip(CircleShape).border(if (isPremiumUserPost) 1.dp else 0.5.dp, if (isPremiumUserPost) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(0.1f), CircleShape),
                        fallbackText = safeName,
                        isAvatar = true
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = safeName,
                                fontWeight = if (isPremiumUserPost) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isPremiumUserPost) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isPremiumUserPost) {
                                Spacer(Modifier.width(6.dp))
                                PremiumBadge()
                            }
                        }
                        Text(
                            text = getRelativeTime(item.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (canDeletePost) {
                            DropdownMenuItem(
                                text = { Text("Sil", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color.Red) },
                                onClick = { onDeletePost(item.id); showMenu = false }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Şikayet Et") },
                                leadingIcon = { Icon(Icons.Filled.Flag, null) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Content
            if (item.content.isNotEmpty() && item.type != FeedType.POLL) {
                if (item.bookTitle == "Şiir") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
                            Icon(Icons.Default.FormatQuote, null, tint = Color(0xFFFFD700).copy(0.5f), modifier = Modifier.size(40.dp))
                            Text(
                                text = item.content,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = item.content,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else if (item.type == FeedType.POLL && item.pollOptions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.content,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val totalVotes = item.pollVotes.values.sum().coerceAtLeast(1)
                    item.pollOptions.forEach { option ->
                        val votes = item.pollVotes[option] ?: 0
                        val percentage = votes.toFloat() / totalVotes
                        val hasVoted = item.pollVoters.contains(currentUserId)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { if (!hasVoted) onVotePoll(item.id, item.pollOptions.indexOf(option)) }
                        ) {
                            if (hasVoted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percentage)
                                        .background(Color(0xFFFFD700).copy(alpha = 0.3f))
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                if (hasVoted) Text("${(percentage * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                            }
                        }
                    }
                    Text("${item.pollVoters.size} oy", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Audio Book integration
            if (item.startPos != null && !item.audioUrl.isNullOrEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp).clip(CircleShape).background(Color(0xFFFFD700))
                                .clickable {
                                    if (isAudioPlaying) onPauseAudio()
                                    else onPlayAudio(item.audioUrl, item.startPos)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAudioLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sesli Alıntı \uD83C\uDFA7", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                            Text(item.bookTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    WaveformProgressBar(progress = audioProgress, isPlaying = isAudioPlaying, modifier = Modifier.fillMaxWidth().height(32.dp), color = Color(0xFFFFD700))
                }
            } else if (item.postImageUrl != null && item.postImageUrl.isNotEmpty()) {
                SmartImage(
                    imageUrl = item.postImageUrl,
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(item.postImageUrl) },
                    contentScale = ContentScale.Crop
                )
            } else if (item.bookTitle != "Şiir" && (item.type == FeedType.LISTEN || (item.bookId.isNotEmpty() && item.bookCoverUrl.isNotEmpty() && item.type != FeedType.POLL))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmartImage(
                        imageUrl = item.bookCoverUrl,
                        modifier = Modifier.width(60.dp).height(88.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (item.type == FeedType.LISTEN) "DİNLENİYOR" else "KİTAP ÖNERİSİ", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700))
                        Text(item.bookTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Actions
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (isLikedLocally) Color.Red.copy(0.15f) else MaterialTheme.colorScheme.onSurface.copy(0.05f))
                        .clickable {
                            if (currentUserId != null) {
                                isLikedLocally = !isLikedLocally
                                likeCountLocally = if (isLikedLocally) likeCountLocally + 1 else (likeCountLocally - 1).coerceAtLeast(0)
                                onToggleLike(item.id, currentUserId, 0)
                            }
                        }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isLikedLocally) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Beğen",
                        tint = if (isLikedLocally) Color.Red else MaterialTheme.colorScheme.onSurface.copy(0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text("$likeCountLocally", style = MaterialTheme.typography.labelMedium, color = if (isLikedLocally) Color.Red else MaterialTheme.colorScheme.onSurface.copy(0.8f))
                }

                if (showCommentSection) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface.copy(0.05f))
                            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Yorum",
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text("${item.commentCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.8f))
                    }
                }
            }
        }
    }
}
