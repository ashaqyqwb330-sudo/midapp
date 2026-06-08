package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Secondary
import com.example.ui.theme.TextGold
import coil.compose.AsyncImage
import java.io.File

@Composable
fun Book3DCard(
    bookTitle: String,
    modifier: Modifier = Modifier,
    coverPath: String? = null,
    activeBaseDir: File? = null,
    onClick: () -> Unit = {}
) {
    var rawRotationX by remember { mutableStateOf(0f) }
    var rawRotationY by remember { mutableStateOf(0f) }

    val rotationX by animateFloatAsState(targetValue = rawRotationX, animationSpec = spring())
    val rotationY by animateFloatAsState(targetValue = rawRotationY, animationSpec = spring())

    val context = androidx.compose.ui.platform.LocalContext.current
    val coverModel = remember(coverPath, activeBaseDir) {
        if (coverPath.isNullOrEmpty()) {
            null
        } else {
            val normalizedCoverPath = coverPath.replace("\\", "/").trim().removePrefix("/")
            var foundFile: File? = null
            if (activeBaseDir != null) {
                val f = File(activeBaseDir, normalizedCoverPath)
                if (f.exists()) {
                    foundFile = f
                }
            }
            if (foundFile == null) {
                val packageName = context.packageName
                val bases = listOfNotNull(
                    context.getExternalFilesDir(null),
                    context.getExternalFilesDir(null)?.let { File(it, "data") },
                    context.filesDir,
                    File(context.filesDir, "data"),
                    File("/storage/emulated/0/Android/data/$packageName/files"),
                    File("/storage/emulated/0/Android/data/$packageName/files/data"),
                    File("/storage/emulated/0/Android/data/com.aistudio.militarymedicallibrary.bchskv/files"),
                    File("/storage/emulated/0/Android/data/com.aistudio.militarymedicallibrary.bchskv/files/data")
                )
                for (base in bases) {
                    val f = File(base, normalizedCoverPath)
                    if (f.exists() && f.isFile) {
                        foundFile = f
                        break
                    }
                }
            }
            foundFile ?: "file:///android_asset/data/$normalizedCoverPath"
        }
    }

    Box(
        modifier = modifier
            .size(130.dp, 195.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            val position = event.changes.firstOrNull()?.position
                            if (position != null) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                rawRotationY = ((position.x - centerX) / centerX) * 18f
                                rawRotationX = ((position.y - centerY) / centerY) * -12f
                            }
                        } else if (event.type == PointerEventType.Exit || event.type == PointerEventType.Release) {
                            rawRotationX = 0f
                            rawRotationY = 0f
                        }
                    }
                }
            }
            .graphicsLayer {
                this.rotationY = rotationY
                this.rotationX = rotationX
                cameraDistance = 12f * density
            }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF162540),
                            Color(0xFF0A1128)
                        )
                    ),
                    RoundedCornerShape(3.dp)
                )
                .border(
                    1.5.dp,
                    Secondary,
                    RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFD4AF37),
                                Color(0xFF9A7B1C)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (coverModel != null) {
                    AsyncImage(
                        model = coverModel,
                        contentDescription = bookTitle,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                    )
                } else {
                    Text(
                        text = "📘",
                        fontSize = 32.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookTitle,
                    color = TextGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
