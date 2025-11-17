package com.veigar.questtracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.veigar.questtracker.model.toComposeColor
import com.veigar.questtracker.model.toHexString


@Composable
fun TwoColorPicker(
    modifier: Modifier = Modifier,
    initialPrimaryColorHex: String,
    initialSecondaryColorHex: String,
    onPrimaryColorSelected: (String) -> Unit, // Returns hex
    onSecondaryColorSelected: (String) -> Unit, // Returns hex
    availableColors: List<Color> = getDefaultMaterialColors()
) {
    var showPrimaryColorDialog by remember { mutableStateOf(false) }
    var showSecondaryColorDialog by remember { mutableStateOf(false) }

    var currentPrimaryColor by remember(initialPrimaryColorHex) {
        mutableStateOf(initialPrimaryColorHex.toComposeColor())
    }
    var currentSecondaryColor by remember(initialSecondaryColorHex) {
        mutableStateOf(initialSecondaryColorHex.toComposeColor())
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Profile Colors",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White, // Adjust if your screen bg is different
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorDisplayItem(
                label = "Primary",
                color = currentPrimaryColor,
                onClick = { showPrimaryColorDialog = true }
            )
            ColorDisplayItem(
                label = "Secondary",
                color = currentSecondaryColor,
                onClick = { showSecondaryColorDialog = true }
            )
        }
    }

    if (showPrimaryColorDialog) {
        ColorSelectionDialog(
            title = "Select Primary Color",
            availableColors = availableColors,
            onColorSelected = { selectedColor ->
                currentPrimaryColor = selectedColor
                onPrimaryColorSelected(selectedColor.toHexString())
                showPrimaryColorDialog = false
            },
            onDismiss = { showPrimaryColorDialog = false }
        )
    }

    if (showSecondaryColorDialog) {
        ColorSelectionDialog(
            title = "Select Secondary Color",
            availableColors = availableColors,
            onColorSelected = { selectedColor ->
                currentSecondaryColor = selectedColor
                onSecondaryColorSelected(selectedColor.toHexString())
                showSecondaryColorDialog = false
            },
            onDismiss = { showSecondaryColorDialog = false }
        )
    }
}

@Composable
private fun ColorDisplayItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .clickable(onClick = onClick)
                .padding(4.dp), // Padding for the border
            contentAlignment = Alignment.Center
        ) {
            // Optional: Show hex code or an icon
            // Text(color.toHexString(), fontSize = 10.sp, color = if (color.luminance() > 0.5) Color.Black else Color.White)
        }
    }
}

@Composable
private fun ColorSelectionDialog(
    title: String,
    availableColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(280.dp)
                    .heightIn(min = 200.dp, max = 450.dp), // Keep height constraints
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 300.dp) // Limit height if many colors
                ) {
                    items(availableColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { onColorSelected(color) }
                                .padding(2.dp), // Inner padding for border effect
                            contentAlignment = Alignment.Center
                        ) {
                            // You could add a checkmark if this color is currently selected
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Provide a list of default colors. You can customize this extensively.
fun getDefaultMaterialColors(): List<Color> {
    return listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
        Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722),
        // Pastel and softer colors
        Color(0xFFB2EBF2), // Light Cyan
        Color(0xFFBCAAA4), // Light Brown
        Color(0xFFC5E1A5), // Light Green
        Color(0xFFFFE0B2), // Light Orange
        Color(0xFFFFCDD2), // Light Pink
        Color(0xFFE1BEE7), // Light Purple
        Color(0xFFCFD8DC), // Blue Grey Light
        Color(0xFFFFF9C4), // Light Yellow
    )
}

fun pickRandomColor(): Color {
    val availableColors = getDefaultMaterialColors()
    return availableColors.random()
}
