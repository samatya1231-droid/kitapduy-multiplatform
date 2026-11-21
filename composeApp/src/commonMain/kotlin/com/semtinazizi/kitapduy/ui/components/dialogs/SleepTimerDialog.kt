package com.semtinazizi.kitapduy.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.ui.components.VipPremiumBadge
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.util.PlaybackTimeFormatter

data class SleepTimerOption(
    val id: String,
    val label: String,
    val minutes: Int? = null,
    val isBookEnd: Boolean = false,
    val isPremium: Boolean = false
)

val sleepTimerOptions = listOf(
    SleepTimerOption(id = "15", label = "15 Dakika", minutes = 15, isPremium = false),
    SleepTimerOption(id = "30", label = "30 Dakika", minutes = 30, isPremium = false),
    SleepTimerOption(id = "45", label = "45 Dakika", minutes = 45, isPremium = false),
    SleepTimerOption(id = "60", label = "1 Saat", minutes = 60, isPremium = true),
    SleepTimerOption(id = "90", label = "90 Dakika", minutes = 90, isPremium = true),
    SleepTimerOption(id = "120", label = "2 Saat", minutes = 120, isPremium = true),
    SleepTimerOption(id = "book_end", label = "Kitap Sonunda", minutes = null, isBookEnd = true, isPremium = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    remainingMinutes: Int,
    onTimerSelected: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    remainingSeconds: Int = remainingMinutes * 60,
    isAtBookEnd: Boolean = false,
    isPremiumUser: Boolean = false,
    onStopAtBookEnd: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasActiveTimer = remainingSeconds > 0 || remainingMinutes > 0 || isAtBookEnd

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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = BrandGold)
                    Text("Uyku Modu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            if (hasActiveTimer) {
                Spacer(modifier = Modifier.height(10.dp))
                val activeLabel = when {
                    isAtBookEnd -> "Zamanlayıcıyı Kapat (Kitap sonunda duracak)"
                    remainingSeconds > 0 -> "Zamanlayıcıyı Kapat (${PlaybackTimeFormatter.formatDurationMs(remainingSeconds * 1000L)} kaldı)"
                    else -> "Zamanlayıcıyı Kapat ($remainingMinutes dk kaldı)"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable {
                            onCancelTimer()
                            onDismiss()
                        }
                        .padding(16.dp)
                        .testTag("sleep-timer-cancel"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        activeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 380.dp)
            ) {
                items(sleepTimerOptions, key = { it.id }) { option ->
                    val locked = option.isPremium && !isPremiumUser
                    Surface(
                        onClick = {
                            if (locked) {
                                onDismiss()
                                onUpgradeClick()
                            } else if (option.isBookEnd) {
                                onStopAtBookEnd()
                                onDismiss()
                            } else if (option.minutes != null) {
                                onTimerSelected(option.minutes)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (locked) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (locked) BorderStroke(1.dp, BrandGold.copy(alpha = 0.45f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("sleep-timer-${option.id}")
                            .semantics(mergeDescendants = true) {}
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (option.isBookEnd) FontWeight.SemiBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (option.isPremium) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (locked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Premium",
                                            tint = BrandGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    VipPremiumBadge(showText = true)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
