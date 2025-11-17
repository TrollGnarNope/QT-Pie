package com.veigar.questtracker.ui.component.createtask

import androidx.compose.foundation.background // Required for Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor // Make sure this is imported
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DifficultyOption(
    val label: String,
    val xp: Int,
    val gold: Int,
    val baseColor: Color,
    val gradientEndColor: Color,
    val starCount: Int
)

val childFriendlyGreen = Color(0xFF66BB6A)
val childFriendlyLightGreen = Color(0xFFA5D6A7)
val childFriendlyOrange = Color(0xFFFFA726)
val childFriendlyLightOrange = Color(0xFFFFCC80)
val childFriendlyBlue = Color(0xFF42A5F5)
val childFriendlyLightBlue = Color(0xFF90CAF9)

// New default colors for unselected boxes
val childFriendlyBoxDefaultBg = Color(0xFFE8F5E9) // A very light, soft minty green as an example
val childFriendlyBoxDefaultContent = Color(0xFF388E41) // A darker green for content on the light mint

val difficultyOptions = listOf(
    DifficultyOption("Easy", 10, 5, childFriendlyGreen, childFriendlyLightGreen, 1),
    DifficultyOption("Medium", 20, 10, childFriendlyOrange, childFriendlyLightOrange, 2),
    DifficultyOption("Hard", 40, 20, childFriendlyBlue, childFriendlyLightBlue, 3)
)

@Composable
fun DifficultySelector(
    selectedXp: Int,
    onDifficultyChange: (xp: Int, gold: Int) -> Unit,
    enabled: Boolean = true
) {
    var currentlySelectedOption by remember {
        mutableStateOf(difficultyOptions.firstOrNull { it.xp == selectedXp } ?: difficultyOptions.first())
    }

    LaunchedEffect(selectedXp) {
        difficultyOptions.firstOrNull { it.xp == selectedXp }?.let {
            currentlySelectedOption = it
        }
    }

    Column {
        Text(
            "Choose Difficulty",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
            color = Color.White
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            difficultyOptions.forEach { option ->
                DifficultyBox(
                    option = option,
                    isSelected = option == currentlySelectedOption,
                    onClick = {
                        if (enabled) {
                            currentlySelectedOption = option
                            onDifficultyChange(option.xp, option.gold)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun DifficultyBox(
    option: DifficultyOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val selectedBrush = Brush.linearGradient(
        colors = listOf(option.baseColor, option.gradientEndColor.copy(alpha = 0.7f))
    )
    // Use the new default child-friendly background for unselected boxes
    val unselectedBrush = SolidColor(childFriendlyBoxDefaultBg)

    // Text color: White on selected (gradient), default content color on unselected
    val textColor = if (isSelected) Color.White else option.baseColor
    // Star color: White on selected, for unselected, let's use the option's base color for a pop,
    // or the default content color if the baseColor clashes with childFriendlyBoxDefaultBg
    val starColorUnselected = option.baseColor // For more colorful stars on default bg
    // val starColorUnselected = childFriendlyBoxDefaultContent // For more muted stars on default bg (choose one)

    val starColor = if (isSelected) Color.White.copy(alpha = 0.9f) else starColorUnselected

    Card(
        modifier = modifier
            .aspectRatio(0.8f) // Or 1f for square
            .clip(RoundedCornerShape(5.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Card itself is transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isSelected) selectedBrush else unselectedBrush,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row {
                    repeat(option.starCount) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = starColor,
                            modifier = Modifier.size(22.sp.value.dp)
                        )
                    }
                }

                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    fontSize = 16.sp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✦ +${option.xp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f), // Slightly transparent for subtlety
                        fontSize = 11.sp
                    )
                    Text(
                        text = "⭐ +${option.gold} Points",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}