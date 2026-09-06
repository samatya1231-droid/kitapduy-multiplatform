package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
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
        var reload by remember { mutableIntStateOf(0) }

        LaunchedEffect(reload) {
            loading = true
            message = null
            try {
                products = billing.loadProducts()
                if (products.isEmpty()) message = "Abonelikler şu anda mağazada görüntülenemiyor. Lütfen daha sonra tekrar deneyin."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = "Abonelikler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin." }
            finally { loading = false }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("KitapDuy Premium") },
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
                Icon(Icons.Default.WorkspacePremium, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Dinleme deneyimini genişlet", style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("Reklamsız kullanım • Çevrimdışı dinleme • Premium ayrıcalıkları",
                    textAlign = TextAlign.Center)
                if (premium) {
                    Text("Bu Apple/Google hesabında aktif Premium aboneliğiniz var.",
                        color = MaterialTheme.colorScheme.primary)
                }
                if (loading || busy) CircularProgressIndicator()
                message?.let { Text(it, textAlign = TextAlign.Center) }
                if (!loading && products.isEmpty()) {
                    OutlinedButton(onClick = { reload++ }, enabled = !busy) { Text("Tekrar Dene") }
                }
                products.forEach { product ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(product.title, style = MaterialTheme.typography.titleLarge)
                            Text("${product.price} / ${product.period}", style = MaterialTheme.typography.headlineSmall)
                            Text("Her abonelik döneminde otomatik yenilenir.")
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy && !loading,
                                onClick = {
                                    busy = true
                                    message = null
                                    scope.launch {
                                        try {
                                            when (billing.startSubscription(product.id)) {
                                                PurchaseResult.SUCCESS -> message = "Satın alma tamamlandı. Premium erişiminiz güncellendi."
                                                PurchaseResult.CANCELLED -> message = "Satın alma iptal edildi."
                                                PurchaseResult.ERROR -> message = "Satın alma tamamlanamadı. Lütfen tekrar deneyin."
                                            }
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (error: Exception) {
                                            message = error.message ?: "Satın alma tamamlanamadı."
                                        } finally { busy = false }
                                    }
                                }
                            ) { Text("Abone Ol") }
                        }
                    }
                }
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
                TextButton(onClick = { navigator.push(LegalScreen(LegalDocument.PRIVACY)) }) {
                    Text("Gizlilik Politikası")
                }
                TextButton(onClick = { navigator.push(LegalScreen(LegalDocument.TERMS)) }) {
                    Text("Kullanım Koşulları")
                }
            }
        }
    }
}
