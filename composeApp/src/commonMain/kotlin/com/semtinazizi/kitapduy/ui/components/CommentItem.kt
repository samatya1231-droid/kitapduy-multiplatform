package com.semtinazizi.kitapduy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semtinazizi.kitapduy.model.Comment

// Utility for relative time in KMP
fun getRelativeTime(timestamp: Long, now: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()): String {
    if (timestamp <= 0L) return ""
    val seconds = ((now - timestamp).coerceAtLeast(0L)) / 1000
    return when {
        seconds < 60 -> "Az önce"
        seconds < 3600 -> "${seconds / 60} dakika önce"
        seconds < 86400 -> "${seconds / 3600} saat önce"
        seconds < 604800 -> "${seconds / 86400} gün önce"
        seconds < 2592000 -> "${seconds / 604800} hafta önce"
        seconds < 31536000 -> "${seconds / 2592000} ay önce"
        else -> "${seconds / 31536000} yıl önce"
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    bookId: String,
    bookTitle: String,
    isPremium: Boolean,
    onEditComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onAddReply: (String) -> Unit,
    onLoadReplies: (String) -> Unit,
    replies: List<Comment>,
    isLoadingReplies: Boolean
) {
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var showReplies by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editCommentText by remember { mutableStateOf(comment.text) }

    val haptic = LocalHapticFeedback.current

    if (showEditDialog) {
        EditCommentDialog(
            initialText = editCommentText,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedText ->
                onEditComment(comment.id, updatedText)
                showEditDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    CommentAvatar(
                        userPhotoUrl = comment.userPhotoUrl,
                        userName = comment.userName,
                        isPremium = comment.isPremium
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        CommentHeader(
                            userName = comment.userName,
                            timestamp = comment.timestamp,
                            isOwner = currentUserId == comment.userId,
                            onMenuClick = { showMenu = true },
                            showMenu = showMenu,
                            onDismissMenu = { showMenu = false },
                            onEditClick = {
                                showMenu = false
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                showMenu = false
                                onDeleteComment(comment.id)
                            }
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            text = comment.text,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            letterSpacing = 0.2.sp
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        CommentActions(
                            isLiked = comment.likedBy.contains(currentUserId),
                            likeCount = comment.likedBy.size,
                            onLikeClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (currentUserId != null) {
                                    onToggleLike(comment.id)
                                }
                            },
                            onReplyClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showReplyInput = !showReplyInput
                            }
                        )
                    }
                }
            }
        }

        if (showReplyInput) {
            ReplyInputSection(
                replyText = replyText,
                onReplyTextChange = { replyText = it },
                onSendReply = {
                    if (replyText.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddReply(replyText)
                        replyText = ""
                        showReplyInput = false
                        showReplies = true
                    }
                }
            )
        }

        if (comment.replyCount > 0) {
            Text(
                text = if (showReplies) "Yanıtları Gizle" else "Yanıtları Gör (${comment.replyCount})",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 60.dp, top = 8.dp).clickable {
                    showReplies = !showReplies
                    if (showReplies && replies.isEmpty()) {
                        onLoadReplies(comment.id)
                    }
                }
            )
        }

        if (showReplies) {
            RepliesSection(
                replies = replies,
                isLoading = isLoadingReplies
            )
        }
    }
}

@Composable
fun EditCommentDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yorumu Düzenle", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Güncelle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun CommentAvatar(
    userPhotoUrl: String?,
    userName: String,
    isPremium: Boolean,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(modifier = Modifier.size(size)) {
        val avatarModifier = Modifier.fillMaxSize().clip(CircleShape)
        SmartImage(
            imageUrl = userPhotoUrl,
            contentDescription = userName,
            modifier = avatarModifier,
            contentScale = ContentScale.Crop,
            isAvatar = true,
            fallbackText = userName
        )
        
        if (isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(2.dp)
            ) {
                Text("👑", fontSize = 8.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CommentHeader(
    userName: String,
    timestamp: Long,
    isOwner: Boolean,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = userName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                getRelativeTime(timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        if (isOwner) {
            Box {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                ) { Icon(Icons.Default.MoreVert, contentDescription = "Yorum seçenekleri", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text("Düzenle") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) },
                        onClick = onEditClick
                    )
                    DropdownMenuItem(
                        text = { Text("Sil", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp)) },
                        onClick = onDeleteClick
                    )
                }
            }
        }
    }
}

@Composable
fun CommentActions(
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val heartScale by animateFloatAsState(
            targetValue = if (isLiked) 1.2f else 1f,
            animationSpec = tween(300),
            label = "heart_scale"
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onLikeClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Beğen",
                tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).scale(heartScale)
            )
            if (likeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "$likeCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onReplyClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Yanıtla",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReplyInputSection(
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 56.dp, top = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = replyText,
            onValueChange = onReplyTextChange,
            placeholder = { Text("Düşüncelerini paylaş...", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSendReply,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Yanıt gönder",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RepliesSection(
    replies: List<Comment>,
    isLoading: Boolean
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.padding(start = 68.dp, top = 16.dp).size(24.dp), 
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        Box(modifier = Modifier.padding(start = 52.dp, top = 8.dp)) {
            val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            Canvas(modifier = Modifier.matchParentSize()) {
                drawLine(
                    color = lineColor,
                    start = Offset(8.dp.toPx(), 0f),
                    end = Offset(8.dp.toPx(), size.height - 24.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Column {
                replies.forEach { reply ->
                    ReplyItem(reply = reply, lineColor = lineColor)
                }
            }
        }
    }
}

@Composable
fun ReplyItem(
    reply: Comment,
    lineColor: Color
) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp)) {
        Canvas(modifier = Modifier.size(24.dp, 24.dp).padding(top = 12.dp)) {
            drawLine(
                color = lineColor,
                start = Offset(8.dp.toPx(), 0f),
                end = Offset(20.dp.toPx(), 0f),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
            cornerRadius = 20.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                CommentAvatar(
                    userPhotoUrl = reply.userPhotoUrl,
                    userName = reply.userName,
                    isPremium = reply.isPremium,
                    size = 32.dp
                )

                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            reply.userName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (reply.isPremium) {
                            Spacer(Modifier.width(4.dp))
                            Text("👑", fontSize = 10.sp)
                        }
                    }
                    Text(
                        reply.text,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
