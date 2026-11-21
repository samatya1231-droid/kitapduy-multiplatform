package com.semtinazizi.kitapduy.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.util.PlaybackTimeFormatter

private const val QUOTE_CLIP_DURATION_MS = 15_000L

private val QUOTE_PRESET_NOTES = listOf(
    "📌 Çok etkilendim",
    "💡 Harika bir tespit",
    "🎧 Mutlaka dinleyin",
    "❤️ Favori bölümüm",
    "✍️ Not aldım"
)

internal fun clampQuoteStartPosition(positionMs: Long, totalDurationMs: Long): Long {
    val safePos = positionMs.coerceAtLeast(0L)
    if (totalDurationMs <= 0L) return safePos
    val maxStart = (totalDurationMs - QUOTE_CLIP_DURATION_MS).coerceAtLeast(0L)
    return safePos.coerceIn(0L, maxStart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDialog(
    onDismiss: () -> Unit,
    bookTitle: String,
    bookAuthor: String,
    bookCoverUrl: String = "",
    userName: String = "KitapDuy Okuru",
    userPhotoUrl: String? = null,
    initialPositionMs: Long = 0L,
    totalDurationMs: Long = 0L,
    isPreviewPlaying: Boolean = false,
    onTogglePreview: ((startPosMs: Long) -> Unit)? = null,
    onPreviewPositionChanged: ((startPosMs: Long) -> Unit)? = null,
    onStopPreview: () -> Unit = {},
    onShare: () -> Unit = {},
    onShareQuoteToFeed: ((quoteText: String, startPosMs: Long) -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var quote by remember { mutableStateOf("") }
    var adjustedPosMs by remember(initialPositionMs, totalDurationMs) {
        mutableLongStateOf(clampQuoteStartPosition(initialPositionMs, totalDurationMs))
    }

    val maxStartMs = if (totalDurationMs > 0L) {
        (totalDurationMs - QUOTE_CLIP_DURATION_MS).coerceAtLeast(0L)
    } else {
        (adjustedPosMs + 60_000L).coerceAtLeast(60_000L)
    }
    val endPosMs = if (totalDurationMs > 0L) {
        (adjustedPosMs + QUOTE_CLIP_DURATION_MS).coerceAtMost(totalDurationMs)
    } else {
        adjustedPosMs + QUOTE_CLIP_DURATION_MS
    }

    DisposableEffect(Unit) {
        onDispose { onStopPreview() }
    }

    fun updateStartPos(newPos: Long) {
        val clamped = clampQuoteStartPosition(newPos, totalDurationMs)
        adjustedPosMs = clamped
        onPreviewPositionChanged?.invoke(clamped)
    }

    ModalBottomSheet(
        onDismissRequest = {
            onStopPreview()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sesli Alıntı Oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {
                        onStopPreview()
                        onDismiss()
                    },
                    modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canlı Alıntı Kartı Önizlemesi (Android VoiceQuoteBottomSheet ile uyumlu)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, BrandGold.copy(alpha = 0.4f)), RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SmartImage(
                                imageUrl = userPhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                isAvatar = true,
                                fallbackText = userName
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = userName.ifBlank { "KitapDuy Okuru" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BrandGold.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = BrandGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Sesli Alıntı 🎧",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = if (quote.isNotBlank()) "\"$quote\"" else "\"Dinlediğin bölümden paylaşmak istediğin notu yaz...\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (quote.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bookCoverUrl.isNotBlank()) {
                            SmartImage(
                                imageUrl = bookCoverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(width = 42.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                fallbackText = bookTitle
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = bookAuthor,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "⏱️ Kesit: ${PlaybackTimeFormatter.formatDurationMs(adjustedPosMs)} - ${PlaybackTimeFormatter.formatDurationMs(endPosMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("quote-clip-range")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ses Kesiti Ayarı (Saniye kırpma ve önizleme kontrolleri)
            Text(
                text = "Ses Kesiti Ayarı (15 Sn Kırpma)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedIconButton(
                            onClick = { updateStartPos(adjustedPosMs - 10_000L) },
                            border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.55f)),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp).testTag("quote-minus-10s")
                        ) {
                            Icon(Icons.Filled.Replay10, contentDescription = "10 saniye geri", tint = BrandGold)
                        }

                        TextButton(
                            onClick = { updateStartPos(adjustedPosMs - 1_000L) },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.heightIn(min = 44.dp).testTag("quote-minus-1s")
                        ) {
                            Text("-1 sn", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onTogglePreview?.invoke(adjustedPosMs) },
                            enabled = onTogglePreview != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 44.dp).testTag("quote-preview-toggle")
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isPreviewPlaying) "Durdur" else "Önizle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        TextButton(
                            onClick = { updateStartPos(adjustedPosMs + 1_000L) },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.heightIn(min = 44.dp).testTag("quote-plus-1s")
                        ) {
                            Text("+1 sn", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedIconButton(
                            onClick = { updateStartPos(adjustedPosMs + 10_000L) },
                            border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.55f)),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp).testTag("quote-plus-10s")
                        ) {
                            Icon(Icons.Filled.Forward10, contentDescription = "10 saniye ileri", tint = BrandGold)
                        }
                    }

                    if (totalDurationMs > 0L && maxStartMs > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Slider(
                            value = adjustedPosMs.toFloat().coerceIn(0f, maxStartMs.toFloat()),
                            onValueChange = { updateStartPos(it.toLong()) },
                            valueRange = 0f..maxStartMs.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Ses kesiti başlangıç saniyesi" },
                            colors = SliderDefaults.colors(
                                thumbColor = BrandGold,
                                activeTrackColor = BrandGold
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                PlaybackTimeFormatter.formatDurationMs(adjustedPosMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                PlaybackTimeFormatter.formatDurationMs(totalDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hızlı Hazır Notlar
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QUOTE_PRESET_NOTES, key = { it }) { preset ->
                    SuggestionChip(
                        onClick = { quote = preset },
                        label = { Text(preset, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = quote,
                onValueChange = { quote = it },
                label = { Text("Alıntın veya notun") },
                modifier = Modifier.fillMaxWidth().testTag("quote-dialog-input"),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(Modifier.height(14.dp))

            Button(
                enabled = quote.isNotBlank(),
                onClick = {
                    val rangeText = "${PlaybackTimeFormatter.formatDurationMs(adjustedPosMs)}-${PlaybackTimeFormatter.formatDurationMs(endPosMs)}"
                    clipboard.setText(AnnotatedString("\"$quote\" ($rangeText)\n— $bookAuthor, $bookTitle"))
                    onStopPreview()
                    onShare()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Alıntıyı kopyala", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            if (onShareQuoteToFeed != null) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    enabled = quote.isNotBlank(),
                    onClick = {
                        onStopPreview()
                        onShareQuoteToFeed(quote.trim(), adjustedPosMs)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quote-share-feed"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Akışta Paylaş", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
