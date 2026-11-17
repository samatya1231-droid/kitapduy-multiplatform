package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.ui.platform.testTag

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.ui.components.SmartImage
import com.semtinazizi.kitapduy.ui.theme.BrandGold
import com.semtinazizi.kitapduy.ui.theme.PlayfairDisplayFamily
import com.semtinazizi.kitapduy.ui.theme.PremiumAccent

internal data class DiscoveryCategory(
    val title: String,
    val color: Color,
    val icon: ImageVector = Icons.AutoMirrored.Rounded.MenuBook
)

internal val genreCategories: List<DiscoveryCategory> = listOf(
    DiscoveryCategory("Roman", Color(0xFF6B2D29), Icons.AutoMirrored.Rounded.MenuBook), // Şarap Rengi
    DiscoveryCategory("Polisiye", Color(0xFF2C3E50), Icons.Rounded.Search), // Gece Mavisi
    DiscoveryCategory("Hikâye", Color(0xFF7B4B3A), Icons.Rounded.AutoStories), // Sıcak Kahve
    DiscoveryCategory("Tarih", Color(0xFF704214), Icons.Rounded.AccountBalance), // Sepya / Kahve
    DiscoveryCategory("Felsefe", Color(0xFF2E4F4F), Icons.Rounded.Psychology), // Koyu Orman Yeşili
    DiscoveryCategory("Bilim Kurgu", Color(0xFF3E4E7A), Icons.Rounded.AutoAwesome), // Derin Uzay Mavisi
    DiscoveryCategory("Bilim", Color(0xFF4A5D7E), Icons.Rounded.Science), // Mat Mavi
    DiscoveryCategory("Şiir", Color(0xFF6E4C5E), Icons.Rounded.AutoAwesome), // Kurumuş Gül
    DiscoveryCategory("Din", Color(0xFF8A795D), Icons.Rounded.AutoStories), // Eskimiş Altın
    DiscoveryCategory("Kişisel Gelişim", Color(0xFF2D6A4F), Icons.Rounded.SelfImprovement), // Zümrüt Yeşili
    DiscoveryCategory("Çocuk Masalları", Color(0xFF965D41), Icons.Rounded.ChildCare), // Kiremit
    DiscoveryCategory("Türk Edebiyatı", Color(0xFF8B3A2B), Icons.Rounded.HistoryEdu), // Bakır Terrakota
    DiscoveryCategory("İngilizce Kitaplar", Color(0xFF1D4E89), Icons.Rounded.Translate) // Lacivert
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
        .replace("I\u0307", "i")
        .replace('İ', 'i')
        .replace('I', 'ı')
        .lowercase()
        .replace("\u0307", "")

internal fun canonicalCategoryKey(name: String): String =
    when (normalizeCategoryName(name)) {
        "hikaye", "hikâye", "öykü", "oyku" -> "hikâye"
        "bilimkurgu", "bilim kurgu", "bilim-kurgu" -> "bilim kurgu"
        "kisisel gelisim", "kişisel gelişim" -> "kişisel gelişim"
        "cocuk", "çocuk", "çocuk masalları", "cocuk masallari", "masal" -> "çocuk masalları"
        "siir", "şiir" -> "şiir"
        "turk edebiyati", "türk edebiyatı" -> "türk edebiyatı"
        "dunya edebiyati", "dünya edebiyatı" -> "dünya edebiyatı"
        "ingilizce", "english", "ingilizce sesli kitap", "ingilizce sesli kitaplar", "ingilizce kitap", "ingilizce kitaplar" -> "ingilizce kitaplar"
        "klasik", "klasikler" -> "klasik"
        else -> normalizeCategoryName(name)
    }

internal fun categoryMatches(actual: String, target: String): Boolean {
    val cleanActual = actual.trim()
    val cleanTarget = target.trim()
    if (cleanActual.isEmpty() || cleanTarget.isEmpty()) return false
    return cleanActual.equals(cleanTarget, ignoreCase = true) ||
        normalizeCategoryName(cleanActual) == normalizeCategoryName(cleanTarget) ||
        canonicalCategoryKey(cleanActual) == canonicalCategoryKey(cleanTarget)
}

internal fun bookMatchesCategory(book: Book, category: String): Boolean {
    if (category.trim().equals("Tümü", ignoreCase = true)) return true
    if (canonicalCategoryKey(category) == "ingilizce kitaplar" && book.language.equals("en", ignoreCase = true)) {
        return true
    }
    if (categoryMatches(book.category, category)) return true
    return book.tags.any { tag -> categoryMatches(tag, category) }
}

internal fun buildActiveDiscoveryCategories(
    books: List<Book>
): Pair<List<DiscoveryCategory>, List<DiscoveryCategory>> {
    if (books.isEmpty()) {
        return genreCategories to collectionCategories
    }

    val distinctCategories = linkedMapOf<String, String>()
    for (book in books) {
        val languageCategories = if (book.language.equals("en", ignoreCase = true)) {
            listOf("İngilizce Kitaplar")
        } else {
            emptyList()
        }
        val rawCategories = listOf(book.category) + book.tags + languageCategories
        for (raw in rawCategories) {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) continue
            val normalized = canonicalCategoryKey(trimmed)
            if (normalized == "podcast") continue
            if (!distinctCategories.containsKey(normalized)) {
                distinctCategories[normalized] = trimmed
            }
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
    initialBooks: List<Book>,
    onCategoryClick: (String) -> Unit,
    onAskida: () -> Unit = {},
    onActivities: () -> Unit = {},
    onAdvisor: () -> Unit = {}
) {
    val (genres, collections) = remember(initialBooks) {
        buildActiveDiscoveryCategories(initialBooks)
    }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Yeni Bir Serüvene Başla + Edebi Danışman kartı (kitapduy3 düzeni)
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }

                LiteraryAdvisorPromoCard(isLight = isLight, onClick = onAdvisor)
            }
        }

        // 2. Kitapların Ötesinde (Edebi Türler'in hemen üstünde — kitapduy3 ile birebir)
        item(span = { GridItemSpan(2) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Kitapların Ötesinde",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlayfairDisplayFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Topluluk ve edebiyat etkinliklerini keşfet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BeyondBooksCardsRow(
                    onArenaClick = onActivities,
                    onAskidaClick = onAskida,
                    isLight = isLight,
                    isCompact = false
                )
            }
        }

        // 3. Edebi Türler
        if (genres.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Edebi Türler",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
            }
            items(genres, key = { it.title }) { category ->
                val matchingBooks = initialBooks.filter { bookMatchesCategory(it, category.title) }
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

        // 4. Özel Seçkiler
        if (collections.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Özel Seçkiler",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                )
            }
            items(collections, key = { it.title }) { category ->
                val matchingBooks = initialBooks.filter { bookMatchesCategory(it, category.title) }
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
    }
}

@Composable
internal fun BeyondBooksCardsRow(
    onArenaClick: () -> Unit,
    onAskidaClick: () -> Unit,
    isLight: Boolean,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModernBeyondBooksCard(
            title = "KitapDuy Arena",
            subtitle = "Edebi düellolar ve yarışmalar",
            badgeText = "ARENA",
            icon = Icons.Rounded.EmojiEvents,
            watermarkIcon = Icons.Rounded.EmojiEvents,
            accentColor = Color(0xFF7C3AED),
            isWarm = false,
            isLight = isLight,
            isCompact = isCompact,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("discover-arena-card"),
            onClick = onArenaClick
        )
        ModernBeyondBooksCard(
            title = "Askıda Kitap",
            subtitle = "Kitap bağışla veya ücretsiz talep et",
            badgeText = "DAYANIŞMA",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            watermarkIcon = Icons.AutoMirrored.Rounded.MenuBook,
            accentColor = Color(0xFFD97706),
            isWarm = true,
            isLight = isLight,
            isCompact = isCompact,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("discover-askida-card"),
            onClick = onAskidaClick
        )
    }
}

@Composable
private fun LiteraryAdvisorPromoCard(
    isLight: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = if (isLight) {
        Brush.horizontalGradient(listOf(Color(0xFFFFFDF7), Color(0xFFFAF3E0)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF231F18), Color(0xFF1D1A14)))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, BrandGold.copy(alpha = if (isLight) 0.45f else 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discover-advisor-button")
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier
                .background(bgBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandGold.copy(alpha = 0.16f))
                    .border(1.dp, BrandGold.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFD49A1A),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Edebi Danışman'a Sor ☕",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ruh haline en uygun sesli kitabı bilge sahafımızla keşfet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD8A62E)
            ) {
                Text(
                    text = "Danış ✨",
                    color = Color(0xFF211703),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
internal fun ModernBeyondBooksCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    watermarkIcon: ImageVector,
    accentColor: Color,
    isWarm: Boolean,
    isLight: Boolean,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val bgColors = when {
        isLight && isWarm -> listOf(Color(0xFFFFFFFF), Color(0xFFFFFBEB), Color(0xFFFEF3C7))
        isLight && !isWarm -> listOf(Color(0xFFFFFFFF), Color(0xFFF5F3FF), Color(0xFFEDE9FE))
        !isLight && isWarm -> listOf(Color(0xFF24180E), Color(0xFF1A1108))
        else -> listOf(Color(0xFF1F1333), Color(0xFF160C26))
    }
    val borderColor = if (isLight) accentColor.copy(alpha = 0.25f) else accentColor.copy(alpha = 0.45f)
    val iconBg = if (isLight) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.2f)
    val badgeBg = when {
        isLight && isWarm -> Color(0xFFFEF3C7)
        isLight && !isWarm -> Color(0xFFEDE9FE)
        else -> accentColor.copy(alpha = 0.22f)
    }
    val badgeTextColor = when {
        isLight && isWarm -> Color(0xFF92400E)
        isLight && !isWarm -> Color(0xFF5B21B6)
        isWarm -> Color(0xFFFDE68A)
        else -> Color(0xFFDDD6FE)
    }

    Box(
        modifier = modifier
            .heightIn(min = if (isCompact) 96.dp else 132.dp)
            .clip(cardShape)
            .background(Brush.linearGradient(bgColors))
            .border(BorderStroke(0.9.dp, borderColor), cardShape)
            .clickable { onClick() }
            .semantics(mergeDescendants = true) {
                role = Role.Button
            }
    ) {
        Icon(
            imageVector = watermarkIcon,
            contentDescription = null,
            modifier = Modifier
                .size(if (isCompact) 48.dp else 68.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
                .rotate(-14f),
            tint = accentColor.copy(alpha = if (isLight) 0.08f else 0.14f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = if (isCompact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 32.dp else 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg)
                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (isCompact) 18.dp else 20.dp),
                        tint = badgeTextColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = badgeBg,
                    border = BorderStroke(0.7.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor,
                            letterSpacing = 0.6.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = badgeTextColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (isCompact) 6.dp else 10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlayfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 13.5.sp else 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (isCompact) 10.sp else 11.5.sp,
                        lineHeight = if (isCompact) 13.sp else 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .testTag("discover-category-$categoryName")
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                    )
                )
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp, end = 64.dp)
            )

            if (bookCount > 0) {
                Text(
                    text = "$bookCount kitap",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 14.dp)
                )
            }

            // Kitap kapağını sağ alta yatık yerleştirme (kitapduy3 ile birebir)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .rotate(-12f)
            ) {
                if (!bookImageUrl.isNullOrBlank()) {
                    SmartImage(
                        imageUrl = bookImageUrl,
                        contentDescription = null,
                        fallbackText = categoryName,
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .shadow(6.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 80.dp)
                            .shadow(6.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
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
