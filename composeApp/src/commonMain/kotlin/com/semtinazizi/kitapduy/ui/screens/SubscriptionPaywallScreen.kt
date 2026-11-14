package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.semtinazizi.kitapduy.platform.BillingManager
import com.semtinazizi.kitapduy.platform.PurchaseResult
import com.semtinazizi.kitapduy.platform.SubscriptionProduct
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

internal enum class SubscriptionPlanType {
    MONTHLY,
    THREE_MONTHS,
    OTHER
}

/**
 * Maps a subscription product ID to a plan type.
 * Rule: If a product id contains "month" and not "3", map to 1-month plan.
 * If it contains "3", map to 3-month plan.
 */
internal fun mapToSubscriptionPlan(productId: String): SubscriptionPlanType {
    val lower = productId.lowercase()
    return when {
        lower.contains("month") && !lower.contains("3") -> SubscriptionPlanType.MONTHLY
        lower.contains("3") -> SubscriptionPlanType.THREE_MONTHS
        else -> SubscriptionPlanType.OTHER
    }
}

internal fun isSameSubscriptionPlan(id1: String, id2: String): Boolean {
    if (id1.equals(id2, ignoreCase = true)) return true
    val plan1 = mapToSubscriptionPlan(id1)
    val plan2 = mapToSubscriptionPlan(id2)
    return plan1 != SubscriptionPlanType.OTHER && plan1 == plan2
}

internal val FALLBACK_PRODUCTS = listOf(
    SubscriptionProduct(
        id = "premium_monthly",
        title = "1 Aylık Premium",
        price = "₺49,99",
        period = "Ay"
    ),
    SubscriptionProduct(
        id = "premium_3months",
        title = "3 Aylık Premium",
        price = "₺119,99",
        period = "3 Ay"
    )
)

internal val FALLBACK_PRODUCTS_WITH_ALIASES = listOf(
    SubscriptionProduct(
        id = "premium_monthly",
        title = "1 Aylık Premium",
        price = "₺49,99",
        period = "Ay"
    ),
    SubscriptionProduct(
        id = "premium_3months",
        title = "3 Aylık Premium",
        price = "₺119,99",
        period = "3 Ay"
    ),
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

internal fun findFallbackProduct(id: String): SubscriptionProduct? {
    return FALLBACK_PRODUCTS_WITH_ALIASES.find { it.id.equals(id, ignoreCase = true) }
        ?: FALLBACK_PRODUCTS.find { isSameSubscriptionPlan(it.id, id) }
}

internal fun normalizeSubscriptionProducts(products: List<SubscriptionProduct>): List<SubscriptionProduct> {
    if (products.isEmpty()) return FALLBACK_PRODUCTS
    val seen = mutableSetOf<SubscriptionPlanType>()
    val result = mutableListOf<SubscriptionProduct>()
    val sorted = products.sortedWith(
        compareBy<SubscriptionProduct> { prod ->
            when (mapToSubscriptionPlan(prod.id)) {
                SubscriptionPlanType.MONTHLY -> 0
                SubscriptionPlanType.THREE_MONTHS -> 1
                SubscriptionPlanType.OTHER -> 2
            }
        }.thenBy { prod ->
            when (prod.id.lowercase()) {
                "premium_monthly", "premium_3months" -> 0
                "premium_1_month", "premium_3_months" -> 1
                else -> 2
            }
        }
    )
    for (p in sorted) {
        val plan = mapToSubscriptionPlan(p.id)
        if (plan == SubscriptionPlanType.OTHER || seen.add(plan)) {
            result.add(p)
        }
    }
    return result
}

internal fun resolveSelectedProduct(
    products: List<SubscriptionProduct>,
    selectedId: String
): SubscriptionProduct? {
    return products.find { it.id.equals(selectedId, ignoreCase = true) }
        ?: products.find { isSameSubscriptionPlan(it.id, selectedId) }
        ?: products.firstOrNull()
}

private val PremiumGoldCta = Color(0xFFD8A648)

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
        var selectedId by remember { mutableStateOf("premium_monthly") }
        val displayProducts = if (products.isNotEmpty()) normalizeSubscriptionProducts(products) else FALLBACK_PRODUCTS
        val selected = resolveSelectedProduct(displayProducts, selectedId)
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        var reload by remember { mutableIntStateOf(0) }

        val canvas = if (dark) Color(0xFF17191D) else Color(0xFFFAF7F1)
        val surface = if (dark) Color(0xFF202227) else Color(0xFFFFFFFF)
        val surfaceAlt = if (dark) Color(0xFF28241C) else Color(0xFFFBF3E4)
        val ink = if (dark) Color(0xFFF3EFE6) else Color(0xFF1D1D1D)
        val muted = if (dark) Color(0xFFC2BBB0) else Color(0xFF5A544C)
        val hairline = if (dark) Color(0xFF33312E) else Color(0xFFE7E1D6)
        val accentText = if (dark) Color(0xFFE9C370) else Color(0xFF705110)
        val accentWash = if (dark) Color(0xFF362D1D) else Color(0xFFF6EBD4)
        val heroColors = if (dark) listOf(Color(0xFF2E271C), Color(0xFF212022)) else listOf(Color(0xFFF4E1BA), Color(0xFFFFF8EA))

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
            } finally {
                loading = false
            }
        }

        Scaffold(
            containerColor = canvas,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = canvas,
                        titleContentColor = ink,
                        navigationIconContentColor = ink
                    ),
                    title = {
                        Text(
                            text = "KitapDuy · PREMIUM",
                            fontFamily = PlayfairDisplayFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = ink)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Card (kitapduy3 20-premium parity)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, hairline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(heroColors))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = accentWash,
                            border = BorderStroke(0.8.dp, accentText.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = accentText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "KitapDuy Premium",
                                    color = accentText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Dinleme keyfine daha fazla yer aç.",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = PlayfairDisplayFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ink
                        )
                        Text(
                            text = "Reklamsız dinleme ve çevrimdışı kitapların, KitapDuy Premium ile bir arada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                            lineHeight = 20.sp
                        )
                    }
                }

                // 2. Plan Seçimi
                Text(
                    text = "Dinleme alışkanlığına uygun plan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ink,
                    modifier = Modifier.fillMaxWidth()
                )

                if (premium) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accentWash,
                        border = BorderStroke(1.dp, accentText.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Bu mağaza hesabında aktif Premium aboneliğiniz var.",
                            color = accentText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (loading || busy) {
                    CircularProgressIndicator(
                        color = accentText,
                        modifier = Modifier.semantics { contentDescription = "Yükleniyor" }
                    )
                }

                message?.let {
                    Text(
                        text = it,
                        textAlign = TextAlign.Center,
                        color = if (it.contains("tamamlandı") || it.contains("yüklendi")) accentText else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                displayProducts.forEach { product ->
                    val isSelected = selected?.id == product.id
                    val isThreeMonths = mapToSubscriptionPlan(product.id) == SubscriptionPlanType.THREE_MONTHS
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {}
                            .selectable(
                                selected = isSelected,
                                enabled = !busy,
                                role = Role.RadioButton,
                                onClick = { selectedId = product.id }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) surfaceAlt else surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PremiumGoldCta else hairline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentText,
                                    unselectedColor = muted
                                ),
                                modifier = Modifier.clearAndSetSemantics {}
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = product.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ink
                                    )
                                    if (isThreeMonths) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accentWash
                                        ) {
                                            Text(
                                                text = "%20 daha avantajlı",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${product.price} / ${product.period}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ink
                                )
                            }
                        }
                    }
                }

                // 3. Bir okuma rotası örneği (kitapduy3 20-premium parity)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surface,
                    border = BorderStroke(1.dp, hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = accentText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Bir okuma rotası örneği",
                                    fontWeight = FontWeight.Bold,
                                    color = ink,
                                    fontSize = 15.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentWash
                            ) {
                                Text(
                                    text = "ÖRNEK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "İnsan doğası · 4 hafta · Haftada 2 oturum",
                            fontSize = 13.sp,
                            color = muted
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = surfaceAlt,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "1. hafta · Karakterin seçimleri",
                                    fontWeight = FontWeight.Bold,
                                    color = ink,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "Odak: Kahramanın kararlarını ve nedenlerini derinlemesine keşfet.",
                                    fontSize = 12.5.sp,
                                    color = muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 4. Avantajlar (2-column cards + Extra features list)
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PaywallBenefitCard(
                        icon = Icons.Default.Headphones,
                        title = "Reklamsız dinle",
                        description = "Sadece sen ve hikâyen.",
                        surface = surface,
                        hairline = hairline,
                        accentWash = accentWash,
                        accentText = accentText,
                        ink = ink,
                        muted = muted,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    PaywallBenefitCard(
                        icon = Icons.Default.Download,
                        title = "Çevrimdışı dinle",
                        description = "İndir, yolda da dinle.",
                        surface = surface,
                        hairline = hairline,
                        accentWash = accentWash,
                        accentText = accentText,
                        ink = ink,
                        muted = muted,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = surface,
                    border = BorderStroke(1.dp, hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Premium ile daha fazlası",
                            fontFamily = PlayfairDisplayFamily,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            fontSize = 16.sp
                        )
                        listOf(
                            "Premium odak sesleri",
                            "Detaylı dinleme analizi",
                            "Özel okur rozeti"
                        ).forEach { feature ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = feature,
                                    color = ink,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 5. CTA + Güven + Yasal bağlantılar
                selected?.let { product ->
                    Text(
                        text = "${product.price} / ${product.period}. İptal edilmediği sürece otomatik yenilenir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !busy && !loading && !premium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremiumGoldCta,
                            contentColor = Color(0xFF1F1608),
                            disabledContainerColor = PremiumGoldCta.copy(alpha = 0.45f),
                            disabledContentColor = Color(0xFF1F1608)
                        ),
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
                        }
                    ) {
                        Text(
                            text = if (busy) "Mağaza işlemi sürüyor…" else if (premium) "Premium aktif" else "Premium aboneliğini başlat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Üyeliğinle bağımsız KitapDuy’un gelişimine de destek olursun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }

                TextButton(
                    onClick = { navigator.pop() },
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Ücretsiz kullanmaya devam et", color = accentText, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                billing.restorePurchases()
                                message = if (billing.isPremium.value) "Premium aboneliğiniz geri yüklendi."
                                else "Bu mağaza hesabında aktif abonelik bulunamadı."
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) {
                                message = "Satın alımlar geri yüklenemedi. Lütfen tekrar deneyin."
                            } finally { busy = false }
                        }
                    }
                ) {
                    Text("Satın Alımları Geri Yükle", color = accentText, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Ödeme mağaza hesabınıza yansıtılır. Abonelik iptal edilmediği sürece seçtiğiniz dönem ve mağazada gösterilen ücretle otomatik yenilenir. Aboneliğinizi cihazınızdaki mağaza hesap ayarlarından yönetebilir veya iptal edebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { navigator.push(LegalScreen(LegalDocument.PRIVACY)) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Gizlilik Politikası", color = accentText, fontWeight = FontWeight.Medium)
                    }
                    TextButton(
                        onClick = { navigator.push(LegalScreen(LegalDocument.TERMS)) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Kullanım Koşulları (EULA)", color = accentText, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallBenefitCard(
    icon: ImageVector,
    title: String,
    description: String,
    surface: Color,
    hairline: Color,
    accentWash: Color,
    accentText: Color,
    ink: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = surface,
        border = BorderStroke(1.dp, hairline),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentWash),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentText, modifier = Modifier.size(18.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ink)
            Text(description, style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}
