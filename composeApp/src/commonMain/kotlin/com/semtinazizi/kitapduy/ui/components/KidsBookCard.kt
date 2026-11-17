package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.model.Book

@Composable
fun KidsBookCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(150.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
    ) {
        Column {
            SmartImage(
                imageUrl = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier.height(130.dp).fillMaxWidth(),
                contentScale = ContentScale.Crop,
                fallbackText = book.title
            )
            Column(Modifier.padding(10.dp)) {
                Text(
                    book.title.ifBlank { "İsimsiz Kitap" },
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Text(book.category, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
