package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.ui.components.SmartImage

@Composable
internal fun ProfileContentTabs(isOwner: Boolean, selected: Int, onSelect: (Int) -> Unit) {
    val labels = if (isOwner) listOf("Kitaplığım", "Paylaşımlarım")
        else listOf("Kitaplığı", "Paylaşımları")
    TabRow(selectedTabIndex = selected) {
        labels.forEachIndexed { index, label ->
            Tab(selected = selected == index, onClick = { onSelect(index) },
                text = { Text(label) })
        }
    }
}

@Composable
internal fun ProfileCoverCard(
    imageUrl: String?, isOwner: Boolean, uploading: Boolean,
    feedback: String? = null, onEdit: () -> Unit, onView: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(2.5f)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = !imageUrl.isNullOrBlank() && !uploading, onClick = onView),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (!imageUrl.isNullOrBlank()) {
                SmartImage(imageUrl = imageUrl, modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
            } else Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Okur köşene bir manzara ekle",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isOwner) {
            OutlinedButton(onClick = onEdit, enabled = !uploading) {
                if (uploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (uploading) "Kapak yükleniyor" else if (imageUrl.isNullOrBlank())
                    "Kapak fotoğrafı ekle" else "Kapak fotoğrafını değiştir")
            }
            if (feedback != null) Text(feedback, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProfileIdentityCard(
    name: String, photoUrl: String?, isOwner: Boolean, membership: String,
    followers: Int, following: Int, uploading: Boolean,
    onPhotoClick: () -> Unit, onEditPhoto: () -> Unit,
    onFollowers: () -> Unit, onFollowing: () -> Unit,
    isPremium: Boolean = false
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box {
                Surface(
                    modifier = Modifier.size(80.dp).clip(CircleShape).clickable(onClick = onPhotoClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isPremium) BorderStroke(2.dp, com.semtinazizi.kitapduy.ui.theme.SleekChampagne) else null
                ) {
                    if (uploading) Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                    } else SmartImage(imageUrl = photoUrl, modifier = Modifier.fillMaxSize(),
                        fallbackText = name, isAvatar = true)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(name, style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false))
                    if (isPremium) {
                        VipPremiumBadge(showText = true)
                    }
                }
                Text(membership, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onFollowers, modifier = Modifier.weight(1f)) {
                Text("$followers takipçi", color = MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = onFollowing, modifier = Modifier.weight(1f)) {
                Text("$following takip", color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (isOwner) {
            TextButton(onClick = onEditPhoto, enabled = !uploading) {
                Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (uploading) "Fotoğraf yükleniyor" else "Profil fotoğrafını değiştir")
            }
        }
    }
}

@Composable
internal fun ProfileEmptyCard(
    title: String, message: String, actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily),
                color = MaterialTheme.colorScheme.onSurface)
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null) {
                OutlinedButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
