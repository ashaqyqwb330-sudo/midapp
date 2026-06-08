package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Secondary

@Composable
fun Shelf(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
            content = content
        )
        // Elegant wood/glass layered shelf
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .shadow(6.dp, RoundedCornerShape(4.dp))
                .background(Color(0x301A2F3F), RoundedCornerShape(4.dp))
                .border(2.dp, Secondary, RoundedCornerShape(4.dp))
        )
    }
}
