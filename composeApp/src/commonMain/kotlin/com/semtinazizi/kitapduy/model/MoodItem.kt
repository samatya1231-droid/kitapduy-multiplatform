package com.semtinazizi.kitapduy.model

import androidx.compose.ui.graphics.Color

data class MoodItem(
    val id: String,
    val name: String,
    val emoji: String,
    val imageUrl: String,
    val color: Color,
    val secondaryColor: Color = color,
    val description: String = "",
    val keywords: List<String> = emptyList()
)

fun getMoodList(): List<MoodItem> {
    return listOf(
        MoodItem(
            id = "peaceful",
            name = "Huzurlu",
            emoji = "🌸",
            imageUrl = "https://images.unsplash.com/photo-1494500764479-0c8f2919a3d8?auto=format&fit=crop&w=400&q=80",
            color = Color(0xFF4CAF50),
            secondaryColor = Color(0xFF80CBC4),
            description = "Zihnini dinlendirecek sakin hikayeler.",
            keywords = listOf("huzur", "sakin", "felsefe", "yoga", "meditasyon", "yaşam", "mutluluk", "masal", "hikaye", "radyo", "doğa")
        ),
        MoodItem(
            id = "curious",
            name = "Meraklı",
            emoji = "🧭",
            imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=400&q=80&cb=1",
            color = Color(0xFF2196F3),
            secondaryColor = Color(0xFF80DEEA),
            description = "Yeni şeyler keşfetmeni sağlayacak bilgiler.",
            keywords = listOf("bilim", "keşif", "teknoloji", "gelecek", "uzay", "araştırma", "insan", "evren", "belgesel", "klasik")
        ),
        MoodItem(
            id = "adventurous",
            name = "Maceracı",
            emoji = "🏔️",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=400&q=80",
            color = Color(0xFFFF5722),
            secondaryColor = Color(0xFFFFAB91),
            description = "Heyecan dolu serüvenler seni bekliyor.",
            keywords = listOf("tarih", "arkeoloji", "evrim", "macera", "dünya", "doğa", "heyecan", "polisiye", "gerilim", "aksiyon")
        ),
        MoodItem(
            id = "romantic",
            name = "Romantik",
            emoji = "❤️",
            imageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&w=400&q=80",
            color = Color(0xFFE91E63),
            secondaryColor = Color(0xFFF48FB1),
            description = "Kalbine dokunacak aşk dolu mısralar.",
            keywords = listOf("edebiyat", "şiir", "sanat", "psikoloji", "insan", "duygu", "roman", "modern", "dizi")
        ),
        MoodItem(
            id = "wise",
            name = "Bilge",
            emoji = "📖",
            imageUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=400&q=80",
            color = Color(0xFFFFC107),
            secondaryColor = Color(0xFFFFE082),
            description = "Yaşamın derinliklerine dair felsefi bakışlar.",
            keywords = listOf("düşünce", "felsefe", "etik", "mantık", "bilgi", "akıl", "toplum", "klasik", "tiyatro", "eser", "sanat")
        ),
        MoodItem(
            id = "mysterious",
            name = "Gizemli",
            emoji = "🔮",
            imageUrl = "https://images.unsplash.com/photo-1501004318641-b39e6451bec6?auto=format&fit=crop&w=400&q=80",
            color = Color(0xFF9C27B0),
            secondaryColor = Color(0xFFCE93D8),
            description = "Sırlarla dolu, merak uyandıran olaylar.",
            keywords = listOf("gizem", "suç", "polis", "psikoloji", "sır", "karanlık", "bilinmez", "gerilim", "korku")
        )
    )
}
