package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.platform.PurchaseResult
import com.semtinazizi.kitapduy.platform.SubscriptionProduct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val FALLBACK_PRODUCTS = listOf(
    SubscriptionProduct(
        id = "premium_1_month",
        title = "1 Aylık Premium",
        price = "₺49,99",
        period = "Ay"
    ),
    SubscriptionProduct(
        id = "premium_3_months",
        title = "3 Aylık Premium",
        price = "₺119,99",
        period = "3 Ay"
    )
)

class SubscriptionPaywallScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val billing = koinInject<BillingManager>()
        val scope = rememberCoroutineScope()
        val premium by billing.isPremium.collectAsState()
        var products by remember { mutableStateOf<List<SubscriptionProduct>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var selectedId by remember { mutableStateOf("premium_1_month") }
        val displayProducts = if (products.isNotEmpty()) products else FALLBACK_PRODUCTS
        val selected = displayProducts.find { it.id == selectedId } ?: displayProducts.firstOrNull()
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        var reload by remember { mutableIntStateOf(0) }

        LaunchedEffect(reload) {
            loading = true
            message = null
            try {
                val loaded = billing.loadProducts()
                if (loaded.isNotEmpty()) {
                    products = loaded
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                // If loading fails, fallback plan cards remain available
            }
            finally { loading = false }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("KitapDuy · PREMIUM") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(if (dark)
                        listOf(Color(0xFF433724), Color(0xFF242321)) else listOf(Color(0xFFF4DFB8), Color(0xFFFFF7E8))))
                        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.WorkspacePremium, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Hikâyene kesintisiz devam et", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Sevdiğin kitapları reklamsız dinle; indirip çevrimdışı da yanında taşı.")
                    }
                }
                PaywallBenefit(Icons.Default.Headphones, "Reklamsız dinle", "Sadece sen ve hikâyen.")
                PaywallBenefit(Icons.Default.Download, "Çevrimdışı dinle", "İndir, yolda da dinle.")
                Text("Dinleme alışkanlığına uygun plan", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth())
                if (premium) {
                    Text("Bu mağaza hesabında aktif Premium aboneliğiniz var.",
                        color = MaterialTheme.colorScheme.primary)
                }
                if (loading || busy) CircularProgressIndicator()
                message?.let {
                    Text(
                        it,
                        textAlign = TextAlign.Center,
                        color = if (it.contains("tamamlandı") || it.contains("yüklendi")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                displayProducts.forEach { product ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected?.id == product.id, enabled = !busy,
                        role = Role.RadioButton, onClick = { selectedId = product.id }),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(if (selected?.id == product.id) 2.dp else 1.dp,
                            if (selected?.id == product.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected?.id == product.id, onClick = null)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(product.title, style = MaterialTheme.typography.titleMedium)
                                Text("${product.price} / ${product.period}", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                Text("Üyeliğinle bağımsız KitapDuy’un gelişimine de destek olursun.", style = MaterialTheme.typography.bodyMedium)
                selected?.let { product ->
                    Text("${product.price} / ${product.period}. İptal edilmediği sürece otomatik yenilenir.", style = MaterialTheme.typography.bodyMedium)
                    Button(modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp), enabled = !busy && !loading && !premium,
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                try {
                                    when (billing.startSubscription(product.id)) {
                                        PurchaseResult.SUCCESS -> message = "Satın alma tamamlandı. Premium erişiminiz güncellendi."
                                        PurchaseResult.CANCELLED -> message = "Satın alma iptal edildi."
                                        PurchaseResult.ERROR -> {
                                            message = "Satın alma tamamlanamadı. Lütfen tekrar deneyin."
                                            if (products.isEmpty()) {
                                                try {
                                                    val loaded = billing.loadProducts()
                                                    if (loaded.isNotEmpty()) products = loaded
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) {
                                    message = error.message ?: "Satın alma tamamlanamadı."
                                    if (products.isEmpty()) {
                                        try {
                                            val loaded = billing.loadProducts()
                                            if (loaded.isNotEmpty()) products = loaded
                                        } catch (_: Exception) {}
                                    }
                                } finally { busy = false }
                            }
                        }) { Text(if (busy) "Mağaza işlemi sürüyor…" else if (premium) "Premium aktif" else "Premium aboneliğini başlat") }
                }
                TextButton(onClick = { navigator.pop() }, enabled = !busy) { Text("Ücretsiz kullanmaya devam et") }
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                billing.restorePurchases()
                                message = if (billing.isPremium.value) "Premium aboneliğiniz geri yüklendi."
                                    else "Bu mağaza hesabında aktif abonelik bulunamadı."
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) { message = "Satın alımlar geri yüklenemedi. Lütfen tekrar deneyin." }
                            finally { busy = false }
                        }
                    }
                ) { Text("Satın Alımları Geri Yükle") }
                Text(
                    "Ödeme mağaza hesabınıza yansıtılır. Abonelik iptal edilmediği sürece seçtiğiniz dönem ve mağazada gösterilen ücretle otomatik yenilenir. Aboneliğinizi cihazınızdaki mağaza hesap ayarlarından yönetebilir veya iptal edebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { navigator.push(LegalScreen(LegalDocument.PRIVACY)) }) {
                        Text("Gizlilik Politikası")
                    }
                    TextButton(onClick = { navigator.push(LegalScreen(LegalDocument.TERMS)) }) {
                        Text("Kullanım Koşulları (EULA)")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallBenefit(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
