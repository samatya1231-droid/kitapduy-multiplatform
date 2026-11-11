package com.semtinazizi.kitapduy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily

@Composable
fun BentoGrid(
    onQuizClick: () -> Unit,
    onPoetryClick: () -> Unit,
    onTvClick: () -> Unit,
    onArticleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BentoItem(
                title = "Bilgi Yarışı",
                subtitle = "Zihnini Test Et",
                vectorIcon = Icons.Default.EmojiEvents,
                color = Color(0xFFFFD700),
                modifier = Modifier.weight(1f),
                onClick = onQuizClick
            )
            BentoItem(
                title = "Odak",
                subtitle = "Zihnini Dinlendir",
                vectorIcon = Icons.Default.AutoStories,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f),
                onClick = onTvClick
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BentoItem(
                title = "Mısralar",
                subtitle = "İlham Verici",
                vectorIcon = Icons.Default.HistoryEdu,
                color = Color(0xFFE040FB),
                modifier = Modifier.weight(1f),
                onClick = onPoetryClick
            )
            BentoItem(
                title = "Keşfet",
                subtitle = "Makaleler",
                vectorIcon = Icons.Default.AutoStories,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f),
                onClick = onArticleClick
            )
        }
    }
}

@Composable
fun BentoItem(
    title: String,
    subtitle: String,
    vectorIcon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val meshOffset = 0f

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    color.copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- Modern Mesh Background Simulation ---
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = (meshOffset - 40).dp, y = (meshOffset - 60).dp)
                    .blur(50.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape)
            )
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (30 - meshOffset / 2).dp, y = (40 - meshOffset / 2).dp)
                    .blur(40.dp)
                    .background(color.copy(alpha = 0.08f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Refined Icon Container
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = color.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = vectorIcon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }
    }
}
