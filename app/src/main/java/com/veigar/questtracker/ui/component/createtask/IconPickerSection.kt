package com.veigar.questtracker.ui.component.createtask // Or your actual package

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.veigar.questtracker.ui.component.AssetCategoryImage
import com.veigar.questtracker.ui.component.listAssetFiles
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark

// Child-friendly colors for the picker
val cfPickerItemBackground = Color(0xFFE0F7FA) // Light cyan
val cfPickerItemSelectedBackground = Color(0xFFB2EBF2) // Slightly darker cyan
val cfPickerItemSelectedBorder = Color(0xFF66BB6A)    // Accent cyan for border
val cfPickerCheckmarkTint = Color(0xFF66BB6A)        // Dark teal for checkmark

@Composable
fun IconPickerSection(
    modifier: Modifier = Modifier,
    selectedIconName: String, // Base name of the icon, e.g., "shopping" (without extension)
    onIconSelected: (String) -> Unit, // Returns base name
    defaultIconExtension: String = ".png", // Assume all picker icons are PNGs
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var showPickerDialog by remember { mutableStateOf(false) }

    // Load available icon base names (without extension)
    val availableIconBaseNames = remember {
        listAssetFiles(context, "categories", removeExtension = true)
    }

    // --- Button to open the picker ---
    Column(modifier = modifier.padding(bottom = 16.dp)){
        Text(
            "Choose Quest Icon", // More engaging title
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White), // Assuming dark parent BG
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = { if (enabled) showPickerDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), // Keep your desired shape
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFADD8E6).copy(alpha = if (enabled) 0.7f else 0.3f), // Example: Light Blue with some transparency
                contentColor = Color.White // Color for the text and icon inside the button
            ),
            enabled = enabled,
            // No border property needed for a filled Button unless you want an additional one
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp) // Keep your padding
        ) {
            // Display the selected icon
            if (selectedIconName.isNotBlank()) {
                AssetCategoryImage(
                    imageNameWithExtension = selectedIconName + defaultIconExtension,
                    contentDescription = "$selectedIconName icon",
                    size = 36.dp // Slightly larger in the button
                )
            } else {
                // Placeholder if no icon is selected yet
                Box(Modifier.size(36.dp).background(Color.Gray.copy(alpha = 0.3f), CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (selectedIconName.isNotBlank()) selectedIconName.replaceFirstChar { it.uppercase() } else "Select Icon",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 18.sp),
                modifier = Modifier.weight(1f)
            )
        }
    }

    // --- Icon Picker Dialog ---
    if (showPickerDialog) {
        Dialog(onDismissRequest = { showPickerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp), // More rounded corners
                colors = CardDefaults.cardColors(containerColor = ProfessionalGrayDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Pick an Icon!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    if (availableIconBaseNames.isEmpty()) {
                        Text("Oh no! No icons found in the treasure chest (assets/categories).")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 60.dp), // Larger tap targets
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(availableIconBaseNames, key = { it }) { iconBaseName ->
                                val isSelected = iconBaseName == selectedIconName
                                IconPickerItem(
                                    iconBaseName = iconBaseName,
                                    iconExtension = defaultIconExtension,
                                    isSelected = isSelected,
                                    onClick = {
                                        onIconSelected(iconBaseName)
                                        showPickerDialog = false
                                    },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { showPickerDialog = false },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                    ) {
                        Text("Maybe Later", style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPickerItem(
    iconBaseName: String,
    iconExtension: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val itemBgColor = if (isSelected) cfPickerItemSelectedBackground else cfPickerItemBackground
    val itemBorder = if (isSelected) BorderStroke(3.dp, cfPickerItemSelectedBorder) else null // Thicker border

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Keep it square
            .clip(RoundedCornerShape(16.dp)) // Nice rounded corners for each item
            .background(itemBgColor)
            .then(if (itemBorder != null) Modifier.border(itemBorder, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(8.dp), // Padding around the icon inside the item
        contentAlignment = Alignment.Center
    ) {
        AssetCategoryImage(
            imageNameWithExtension = iconBaseName + iconExtension,
            contentDescription = iconBaseName,
            size = 32.dp,
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = cfPickerCheckmarkTint,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp) // Prominent checkmark
                    .background(itemBgColor.copy(alpha = 0.7f), CircleShape) // Semi-transparent bg for check
            )
        }
    }
}