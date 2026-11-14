package com.semtinazizi.kitapduy.ui.screens
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily

internal data class DiscoveryCategory(
    val title: String,
    val color: Color,
    val icon: ImageVector = Icons.AutoMirrored.Rounded.MenuBook
)

internal val genreCategories: List<DiscoveryCategory> = listOf(
    DiscoveryCategory("Roman", Color(0xFF6B2D29), Icons.AutoMirrored.Rounded.MenuBook), // Şarap Rengi
    DiscoveryCategory("Türk Edebiyatı", Color(0xFF8B3A2B), Icons.Rounded.HistoryEdu), // Bakır Terrakota
    DiscoveryCategory("Polisiye", Color(0xFF2C3E50), Icons.Rounded.Search), // Gece Mavisi
    DiscoveryCategory("Tarih", Color(0xFF704214), Icons.Rounded.AccountBalance), // Sepya / Kahve
    DiscoveryCategory("Felsefe", Color(0xFF2E4F4F), Icons.Rounded.Psychology), // Koyu Orman Yeşili
    DiscoveryCategory("Bilim", Color(0xFF4A5D7E), Icons.Rounded.Science), // Mat Mavi
    DiscoveryCategory("Şiir", Color(0xFF6E4C5E), Icons.Rounded.AutoAwesome), // Kurumuş Gül
    DiscoveryCategory("Din", Color(0xFF8A795D), Icons.Rounded.AutoStories), // Eskimiş Altın
    DiscoveryCategory("Kişisel Gelişim", Color(0xFF2D6A4F), Icons.Rounded.SelfImprovement), // Zümrüt Yeşili
    DiscoveryCategory("İngilizce Kitaplar", Color(0xFF1D4E89), Icons.Rounded.Translate), // Lacivert
    DiscoveryCategory("Çocuk Masalları", Color(0xFF965D41), Icons.Rounded.ChildCare) // Kiremit
)

internal val collectionCategories: List<DiscoveryCategory> = listOf(
    DiscoveryCategory("Dünya Edebiyatı", Color(0xFF453051), Icons.Rounded.Public), // Derin Mor
    DiscoveryCategory("Klasik", Color(0xFF1C2833), Icons.Rounded.WorkspacePremium) // Mürekkep Siyahı
)

private val fallbackCategoryStyles: List<Pair<Color, ImageVector>> = listOf(
    Color(0xFF3E4E5E) to Icons.AutoMirrored.Rounded.MenuBook,
    Color(0xFF5C4742) to Icons.Rounded.AutoStories,
    Color(0xFF355C54) to Icons.Rounded.Explore,
    Color(0xFF5A4366) to Icons.Rounded.AutoAwesome
)

internal fun normalizeCategoryName(name: String): String =
    name.trim()
        .replace('İ', 'i')
        .replace('I', 'ı')
        .lowercase()
        .replace("\u0307", "")

internal fun categoryMatches(actual: String, target: String): Boolean {
    val cleanActual = actual.trim()
    val cleanTarget = target.trim()
    if (cleanActual.isEmpty() || cleanTarget.isEmpty()) return false
    return cleanActual.equals(cleanTarget, ignoreCase = true) ||
        normalizeCategoryName(cleanActual) == normalizeCategoryName(cleanTarget)
}

internal fun buildActiveDiscoveryCategories(
    books: List<Book>
): Pair<List<DiscoveryCategory>, List<DiscoveryCategory>> {
    if (books.isEmpty()) {
        return genreCategories to collectionCategories
    }

    val distinctCategories = linkedMapOf<String, String>()
    for (book in books) {
        val trimmed = book.category.trim()
        if (trimmed.isBlank()) continue
        val normalized = normalizeCategoryName(trimmed)
        if (normalized == "podcast") continue
        if (!distinctCategories.containsKey(normalized)) {
            distinctCategories[normalized] = trimmed
        }
    }

    if (distinctCategories.isEmpty()) {
        return genreCategories to collectionCategories
    }

    val matchedGenres = genreCategories.filter { genre ->
        distinctCategories.containsKey(normalizeCategoryName(genre.title))
    }
    val matchedCollections = collectionCategories.filter { collection ->
        distinctCategories.containsKey(normalizeCategoryName(collection.title))
    }

    val knownKeys = (matchedGenres + matchedCollections)
        .map { normalizeCategoryName(it.title) }
        .toSet()

    val extraGenres = distinctCategories.entries
        .filter { (normalized, _) -> normalized !in knownKeys }
        .mapIndexed { index, (_, originalTitle) ->
            val (color, icon) = fallbackCategoryStyles[index % fallbackCategoryStyles.size]
            DiscoveryCategory(title = originalTitle, color = color, icon = icon)
        }

    return (matchedGenres + extraGenres) to matchedCollections
}

@Composable
internal fun DiscoverCategories(
    initialBooks: List<Book>, onCategoryClick: (String) -> Unit,
    onAskida: () -> Unit = {}, onActivities: () -> Unit = {}
) {
    val (genres, collections) = remember(initialBooks) {
        buildActiveDiscoveryCategories(initialBooks)
    }
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
                if (genres.isNotEmpty()) {
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
        }
        items(genres, key = { it.title }) { category ->
            val matchingBooks = initialBooks.filter { categoryMatches(it.category, category.title) }
            DiscoveryCategoryCard(
                categoryName = category.title,
                backgroundColor = category.color,
                icon = category.icon,
                bookCount = matchingBooks.size,
                bookImageUrl = matchingBooks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                onClick = { onCategoryClick(category.title) }
            )
        }
        if (collections.isNotEmpty()) {
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
                val matchingBooks = initialBooks.filter { categoryMatches(it.category, category.title) }
                DiscoveryCategoryCard(
                    categoryName = category.title,
                    backgroundColor = category.color,
                    icon = category.icon,
                    bookCount = matchingBooks.size,
                    bookImageUrl = matchingBooks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                    onClick = { onCategoryClick(category.title) }
                )
            }
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
                            .height(130.dp)
                            .semantics(mergeDescendants = true) {},
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
                            .height(130.dp)
                            .semantics(mergeDescendants = true) {},
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
internal fun DiscoveryCategoryCard(
    categoryName: String,
    backgroundColor: Color,
    icon: ImageVector = Icons.AutoMirrored.Rounded.MenuBook,
    bookCount: Int = 0,
    bookImageUrl: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-6).dp, y = 10.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp, end = 64.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (bookCount > 0) {
                    Text(
                        text = "$bookCount eser",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            // Kitap kapağını sağ alta yatık yerleştirme (Spotify tarzı)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 18.dp, y = 12.dp)
                    .rotate(-22f)
            ) {
                if (!bookImageUrl.isNullOrBlank()) {
                    SmartImage(
                        imageUrl = bookImageUrl,
                        contentDescription = null,
                        fallbackText = categoryName,
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
