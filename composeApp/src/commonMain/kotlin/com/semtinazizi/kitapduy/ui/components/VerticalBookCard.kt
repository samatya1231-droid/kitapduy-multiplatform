package com.semtinazizi.kitapduy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.semtinazizi.kitapduy.model.Book

@Composable
fun VerticalBookCard(book: Book, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")

    Column(
        modifier = Modifier
            .width(160.dp)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 1. Ambient Drop Shadow Layer
            AsyncImage(
                model = book.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.85f)
                    .offset(y = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .blur(20.dp)
                    .graphicsLayer { alpha = 0.8f }
            )

            // 2. The Card Container
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
            ) {}

            // 3. Floating Cover Image
            Card(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Refined Typography Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
            .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = book.title.ifBlank { "İsimsiz Kitap" },
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = book.author.ifBlank { "Bilinmeyen Yazar" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
