package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne

@Composable
fun PremiumUpgradeCard(
    modifier: Modifier = Modifier,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    if (isPremium) return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = if (isDark) Color(0xFF242018) else Color(0xFFFFFAF0)
    val titleColor = if (isDark) Color(0xFFF5EFE6) else Color(0xFF1F1A14)
    val subtitleColor = if (isDark) Color(0xFFC8BFB4) else Color(0xFF574E43)
    val badgeBg = if (isDark) SleekChampagne.copy(alpha = 0.16f) else Color(0xFFF7EEDB)
    val badgeText = if (isDark) SleekChampagne else Color(0xFF6D4C2D)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        border = BorderStroke(1.dp, SleekChampagne.copy(alpha = 0.42f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(50),
                border = BorderStroke(0.5.dp, SleekChampagne.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "✨ KitapDuy Premium",
                    color = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Dinleme keyfine daha fazla yer aç.",
                color = titleColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Reklamsız akış, çevrimdışı kütüphane ve Edebi Danışman için daha geniş günlük kullanım alanı.",
                color = subtitleColor,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE9BD6A),
                    contentColor = Color(0xFF30200B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "14 gün ücretsiz dene",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
