package com.semtinazizi.kitapduy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.semtinazizi.kitapduy.ui.theme.SleekChampagne

@Composable
fun HomeAdvisorFab(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(28.dp)
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        shape = shape,
        containerColor = if (isDark) Color(0xFFF2D99C) else Color(0xFF72502D),
        contentColor = if (isDark) Color(0xFF302414) else Color(0xFFFFF7E7),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 5.dp
        ),
        modifier = modifier
            .testTag("home-advisor-fab")
            .border(BorderStroke(1.dp, SleekChampagne.copy(alpha = 0.35f)), shape)
            .semantics(mergeDescendants = true) {},
        icon = {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = if (expanded) null else "Danışmana sor",
                modifier = Modifier.size(20.dp)
            )
        },
        text = {
            Text(
                text = "Danışmana sor",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}
