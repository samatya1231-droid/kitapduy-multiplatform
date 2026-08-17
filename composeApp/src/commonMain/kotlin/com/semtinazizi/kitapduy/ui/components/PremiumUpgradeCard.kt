package com.semtinazizi.kitapduy.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valentinilk.shimmer.shimmer

// Not using Icons.Default.Diamond because it might not be in standard material icons commonMain. 
// Using Icons.Filled.Star as a fallback if Diamond is not available, but let's try Diamond first if it's in extended.
// Since materialIconsExtended is in gradle, Diamond should be available.
import androidx.compose.material.icons.filled.Star

@Composable
fun PremiumUpgradeCard(
    modifier: Modifier = Modifier,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    if (isPremium) return

    val brandGold = Color(0xFFFFD700)
    val brandPremiumDark = Color(0xFF121212) // Always dark for VIP feel
    val brandCopper = Color(0xFFB87333)
    val textColorPremium = Color(0xFFF5F5F5)
    
    val shimmerAlpha = 0.5f
    val scale = 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = brandPremiumDark,
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(brandGold, brandCopper, brandGold))),
        shadowElevation = 16.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Decorative Element (Large Blurred Icon)
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp, y = 40.dp)
                    .blur(60.dp)
                    .graphicsLayer(alpha = 0.1f, rotationZ = -20f),
                tint = brandGold
            )

            // Dynamic Glow Blobs
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .background(brandGold.copy(alpha = 0.08f), CircleShape)
                    .blur(50.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = brandGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, brandGold.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = brandGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "KitapDuy Premium",
                                color = textColorPremium,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Surface(
                                color = brandGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "VIP DENEYİM",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Sınırları kaldır, reklamsız dinle ve çevrimdışı kütüphaneni oluştur.",
                    color = textColorPremium.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shimmer(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, brandGold.copy(alpha = shimmerAlpha))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(brandGold, Color(0xFFFFE082))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "7 GÜN ÜCRETSİZ DENE",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "*Deneme sonunda aylık 49,99 ₺. İstediğin zaman iptal edebilirsin.",
                    color = textColorPremium.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
