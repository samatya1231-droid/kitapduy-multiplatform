package com.semtinazizi.kitapduy.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AtmosphereSound(val name: String, val url: String, val icon: ImageVector, val isPremium: Boolean = false)

val atmosphereSounds = listOf(
    AtmosphereSound("Yağmur", "https://ancient-shape-df97.samatya231.workers.dev/rain.mp3", Icons.Default.WaterDrop),
    AtmosphereSound("Kafe", "https://ancient-shape-df97.samatya231.workers.dev/cafe.mp3", Icons.Default.LocalCafe),
    AtmosphereSound("Şömine", "https://ancient-shape-df97.samatya231.workers.dev/fire.mp3", Icons.Default.LocalFireDepartment),
    AtmosphereSound("Rüzgar", "https://ancient-shape-df97.samatya231.workers.dev/wind.mp3", Icons.Default.Air, isPremium = true)
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Atmosfer Sesleri", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSoundSelected(null) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (currentSound == null) com.semtinazizi.kitapduy.ui.theme.BrandGold else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = if (currentSound == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kapalı", style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(atmosphereSounds) { sound ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            if (sound.isPremium && !isPremiumUser) {
                                onDismiss()
                                onUpgradeClick()
                            } else {
                                onSoundSelected(sound.url) 
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (currentSound == sound.url) com.semtinazizi.kitapduy.ui.theme.BrandGold else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(sound.icon, contentDescription = sound.name, tint = if (currentSound == sound.url) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            if (sound.isPremium && !isPremiumUser) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Premium",
                                    tint = com.semtinazizi.kitapduy.ui.theme.BrandGold,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(sound.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (currentSound != null) {
                Text("Ses Seviyesi", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = com.semtinazizi.kitapduy.ui.theme.BrandGold,
                        activeTrackColor = com.semtinazizi.kitapduy.ui.theme.BrandGold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
