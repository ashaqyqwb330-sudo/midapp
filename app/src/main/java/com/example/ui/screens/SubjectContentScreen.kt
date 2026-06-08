package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun SubjectContentScreen(
    subjectTitle: String,
    onBack: () -> Unit,
    onContentTypeClick: (contentType: String) -> Unit
) {
    val types = listOf(
        Triple("النظري", "📘", Secondary),
        Triple("العملي", "🔬", Color(0xFFF39C12)),
        Triple("المرجع", "📚", Color(0xFF2980B9))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF020810), Primary, Color(0xFF0F1F33))))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔙",
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = subjectTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextGold,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Multi-column or Row layout adaptive for all screen sizes
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            types.forEach { (type, icon, sColor) ->
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .clickable { onContentTypeClick(type) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = icon, fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = type,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = sColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
