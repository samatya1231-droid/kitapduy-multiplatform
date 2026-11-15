package com.semtinazizi.kitapduy.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDialog(
    onDismiss: () -> Unit,
    bookTitle: String,
    bookAuthor: String,
    onShare: () -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current
    var quote by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alıntı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.semtinazizi.kitapduy.ui.theme.BrandGold.copy(alpha = 0.2f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Dinlediğin bölümden hatırlamak istediğin cümleyi yaz.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$bookTitle\n$bookAuthor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(value = quote, onValueChange = { quote = it },
                label = { Text("Alıntın") }, modifier = Modifier.fillMaxWidth().testTag("quote-dialog-input"), minLines = 3)
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = quote.isNotBlank(),
                onClick = {
                    clipboard.setText(AnnotatedString("\"$quote\"\n— $bookAuthor, $bookTitle"))
                    onShare()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.semtinazizi.kitapduy.ui.theme.BrandGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Alıntıyı kopyala", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
