package com.semtinazizi.kitapduy.ui.screens
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily

private data class DiscoveryCategory(val title: String, val color: Color)

@Composable
internal fun DiscoverCategories(
    initialBooks: List<Book>, onCategoryClick: (String) -> Unit,
    onAskida: () -> Unit = {}, onActivities: () -> Unit = {}
) {
    val genres = listOf(
        DiscoveryCategory("Roman", Color(0xFF6B2D29)), // Şarap Rengi
        DiscoveryCategory("Polisiye", Color(0xFF2C3E50)), // Gece Mavisi
        DiscoveryCategory("Tarih", Color(0xFF704214)), // Sepya / Kahve
        DiscoveryCategory("Felsefe", Color(0xFF2E4F4F)), // Koyu Orman Yeşili
        DiscoveryCategory("Bilim", Color(0xFF4A5D7E)), // Mat Mavi
        DiscoveryCategory("Şiir", Color(0xFF6E4C5E)), // Kurumuş Gül
        DiscoveryCategory("Din", Color(0xFF8A795D)), // Eskimiş Altın
        DiscoveryCategory("Çocuk Masalları", Color(0xFF965D41)) // Kiremit
    )
    val collections = listOf(
        DiscoveryCategory("Dünya Edebiyatı", Color(0xFF453051)), // Derin Mor
        DiscoveryCategory("Klasik", Color(0xFF1C2833)) // Mürekkep Siyahı
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Yeni Bir Serüvene Başla",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Kitaplığınızın sınırlarını genişletin.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Edebi Türler",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
        }
        items(genres, key = { it.title }) { category ->
            DiscoveryCategoryCard(
                categoryName = category.title,
                backgroundColor = category.color,
                bookImageUrl = initialBooks.firstOrNull { it.coverUrl.isNotBlank() && it.category.contains(category.title, true) }?.coverUrl,
                onClick = { onCategoryClick(category.title) }
            )
        }
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Özel Seçkiler",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
        items(collections, key = { it.title }) { category ->
            DiscoveryCategoryCard(
                categoryName = category.title,
                backgroundColor = category.color,
                bookImageUrl = initialBooks.firstOrNull { it.coverUrl.isNotBlank() && it.category.contains(category.title, true) }?.coverUrl,
                onClick = { onCategoryClick(category.title) }
            )
        }
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                Text(
                    text = "Kitapların Ötesinde",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Askıda Kitap Kartı
                    Surface(
                        onClick = onAskida,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Arka plan deseni/ikonu (büyük ve silik) - Burada varsayılan bir ikon veya resim kullanılabilir
                            // Şimdilik standart kitap ikonunu arka planda hafifçe döndürerek kullanıyoruz.
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 16.dp, y = 16.dp)
                                    .rotate(-15f),
                                tint = PremiumAccent.copy(alpha = 0.1f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PremiumAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = PremiumAccent
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Askıda Kitap",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = PlayfairDisplayFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Paylaş ve keşfet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Edebi Meydan Kartı
                    Surface(
                        onClick = onActivities,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Arka plan deseni/ikonu (büyük ve silik)
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 16.dp, y = 16.dp)
                                    .rotate(-15f),
                                tint = PremiumAccent.copy(alpha = 0.1f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PremiumAccent.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = PremiumAccent
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Edebi Meydan",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = PlayfairDisplayFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Düellolar ve testler",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DiscoveryCategoryCard(categoryName: String, backgroundColor: Color,
    bookImageUrl: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            )

            // Kitap kapağını sağ alta yatık yerleştirme (Spotify tarzı)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 18.dp, y = 12.dp)
                    .rotate(-22f)
            ) {
                SmartImage(
                    imageUrl = bookImageUrl,
                    fallbackText = categoryName,
                    modifier = Modifier
                        .size(width = 56.dp, height = 80.dp)
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
