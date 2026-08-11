package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.platform.PurchaseResult
import com.semtinazizi.kitapduy.ui.theme.PremiumBronze
import com.semtinazizi.kitapduy.ui.theme.PremiumCopper
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class PremiumFeature(
    val title: String,
    val description: String,
    val icon: ImageVector
)

val premiumFeatures = listOf(
    PremiumFeature("Altın Rozet & İsim", "Toplulukta ayrıcalıklı statünüzü sergileyin.", Icons.Default.WorkspacePremium),
    PremiumFeature("Sıfır Reklam", "Hiçbir kesinti olmadan edebiyatın tadını çıkarın.", Icons.Default.Block),
    PremiumFeature("Çevrimdışı Dinle", "İçerikleri indirin, internetsiz her yerde dinleyin.", Icons.Default.Download),
    PremiumFeature("Premium Analiz", "Okuma ve dinleme alışkanlıklarınızı keşfedin.", Icons.Default.Insights),
    PremiumFeature("Gelişmiş Deneyim", "Özel renk temaları ve ekstra özellikler.", Icons.Default.AutoAwesome)
)

class SubscriptionPaywallScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val billingManager = koinInject<BillingManager>()
        val uriHandler = LocalUriHandler.current
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SOCIAL PROOF
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = PremiumBronze,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "50.000+ Premium Üye",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Kütüphanenin Tüm\nKapılarını Açın",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Bütün özellikleri alt alta listeliyoruz (KMP'de Pager yapısı ek kütüphane gerektirmesin diye şimdilik sütun yapalım)
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    premiumFeatures.take(3).forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PremiumCopper.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    feature.icon,
                                    contentDescription = null,
                                    tint = PremiumBronze,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // PRICING CARDS
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PricingCard(
                        title = "Yıllık Plan",
                        price = "Yıllık Ödeme",
                        description = "İlk 7 Gün Ücretsiz Deneyin",
                        isPopular = true,
                        onClick = {
                            coroutineScope.launch {
                                val result = billingManager.startSubscription("premium_yearly")
                                if (result == PurchaseResult.SUCCESS) {
                                    navigator.pop()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PricingCard(
                        title = "Aylık Plan",
                        price = "Aylık Ödeme",
                        description = "İlk 7 Gün Ücretsiz • İstediğin zaman iptal et",
                        onClick = {
                            coroutineScope.launch {
                                val result = billingManager.startSubscription("premium_monthly")
                                if (result == PurchaseResult.SUCCESS) {
                                    navigator.pop()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Abonelik otomatik olarak yenilenir. İstediğiniz zaman abonelik ayarlarından iptal edebilirsiniz.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { uriHandler.openUri("https://kitapduy.app/privacy") }) {
                        Text(
                            "Gizlilik Politikası",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "|",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    TextButton(onClick = { uriHandler.openUri("https://kitapduy.app/terms") }) {
                        Text(
                            "Kullanım Koşulları",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PricingCard(
    title: String,
    price: String,
    description: String,
    isPopular: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isPopular) PremiumCopper.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.05f
        ),
        border = if (isPopular) BorderStroke(1.5.dp, PremiumBronze) else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isPopular) FontWeight.ExtraBold else FontWeight.Bold
                )
                if (isPopular) {
                    Surface(
                        color = PremiumBronze,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "AVANTAJLI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isPopular) PremiumBronze else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPopular) PremiumBronze.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.6f
                ),
                fontWeight = if (isPopular) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
