package com.semtinazizi.kitapduy.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semtinazizi.kitapduy.ui.components.VipPremiumBadge
import com.semtinazizi.kitapduy.ui.theme.BrandGold

data class AtmosphereSound(
    val id: String,
    val name: String,
    val resourceName: String,
    val fallbackUrl: String,
    val icon: ImageVector,
    val isPremium: Boolean = false
)

val atmosphereSounds = listOf(
    AtmosphereSound(
        id = "rain",
        name = "Yağmur",
        resourceName = "rain_sound",
        fallbackUrl = "https://ses.kitapduy.com.tr/rain_sound.mp3",
        icon = Icons.Default.WaterDrop,
        isPremium = false
    ),
    AtmosphereSound(
        id = "fire",
        name = "Şömine",
        resourceName = "fire_sound",
        fallbackUrl = "https://ses.kitapduy.com.tr/fire_sound.mp3",
        icon = Icons.Default.LocalFireDepartment,
        isPremium = false
    ),
    AtmosphereSound(
        id = "cafe",
        name = "Kafe",
        resourceName = "cafe_sound",
        fallbackUrl = "https://ses.kitapduy.com.tr/cafe_sound.mp3",
        icon = Icons.Default.LocalCafe,
        isPremium = true
    ),
    AtmosphereSound(
        id = "forest",
        name = "Gece Ormanı",
        resourceName = "forest_sound",
        fallbackUrl = "https://ses.kitapduy.com.tr/forest_sound.mp3",
        icon = Icons.Default.Forest,
        isPremium = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosphereDialog(
    onDismiss: () -> Unit,
    currentSound: String?,
    onSoundSelected: (String?) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    isPremiumUser: Boolean,
    onUpgradeClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Atmosfer Sesleri",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isPremiumUser) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Kafe ve Gece Ormanı Premium üyelere özeldir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item(key = "atmosphere_off") {
                    val selected = currentSound == null
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(min = 68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSoundSelected(null) }
                            .padding(vertical = 4.dp)
                            .testTag("atmosphere-off")
                            .semantics(mergeDescendants = true) {}
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (selected) BrandGold else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapalı",
                                tint = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Kapalı",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                items(atmosphereSounds, key = { it.id }) { sound ->
                    val selected = currentSound == sound.id || currentSound == sound.fallbackUrl
                    val locked = sound.isPremium && !isPremiumUser
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(min = 72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (locked) {
                                    onDismiss()
                                    onUpgradeClick()
                                } else {
                                    onSoundSelected(sound.id)
                                }
                            }
                            .padding(vertical = 4.dp)
                            .testTag("atmosphere-${sound.id}")
                            .semantics(mergeDescendants = true) {}
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (selected) BrandGold else MaterialTheme.colorScheme.surfaceVariant)
                                .then(
                                    if (locked) Modifier.border(1.5.dp, BrandGold.copy(alpha = 0.7f), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                sound.icon,
                                contentDescription = sound.name,
                                tint = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                            if (locked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1A1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Premium",
                                        tint = BrandGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            sound.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (sound.isPremium) {
                            Spacer(modifier = Modifier.height(3.dp))
                            VipPremiumBadge(showText = true)
                        }
                    }
                }
            }

            if (currentSound != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ses Seviyesi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${(volume.coerceIn(0f, 1f) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = volume.coerceIn(0f, 1f),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandGold,
                        activeTrackColor = BrandGold
                    )
                )
            } else if (!isPremiumUser) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = {
                        onDismiss()
                        onUpgradeClick()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = BrandGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tüm atmosfer seslerini aç",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        VipPremiumBadge(showText = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
