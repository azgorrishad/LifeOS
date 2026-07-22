package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuickNotesOverlay() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("quick_notes_prefs", Context.MODE_PRIVATE) }
    
    var isExpanded by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(sharedPrefs.getString("quick_note", "") ?: "") }
    
    // Save to SharedPreferences on change
    LaunchedEffect(noteText) {
        sharedPrefs.edit().putString("quick_note", noteText).apply()
    }

    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        
        var offsetX by remember { mutableFloatStateOf(maxWidthPx - with(density) { 70.dp.toPx() }) }
        var offsetY by remember { mutableFloatStateOf(maxHeightPx - with(density) { 320.dp.toPx() }) }

        // Ensure we don't go out of bounds on recomposition
        offsetX = offsetX.coerceIn(0f, (maxWidthPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (maxHeightPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f))

        val dragModifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val newX = offsetX + dragAmount.x
                val newY = offsetY + dragAmount.y
                offsetX = newX.coerceIn(0f, (maxWidthPx - 56.dp.toPx()).coerceAtLeast(0f))
                offsetY = newY.coerceIn(0f, (maxHeightPx - 56.dp.toPx()).coerceAtLeast(0f))
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = fadeOut() + scaleOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                ) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .height(250.dp)
                            .padding(bottom = 8.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(dragModifier),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Notes (Drag)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { isExpanded = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (noteText.isEmpty()) {
                                            Text(
                                                text = "Type something...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(56.dp).then(dragModifier),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        if (isExpanded) Icons.Rounded.Close else Icons.Rounded.Edit,
                        contentDescription = "Quick Notes"
                    )
                }
            }
        }
    }
}
