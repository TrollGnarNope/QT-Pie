package com.veigar.questtracker.ui.component.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Simpler Add icon
import androidx.compose.material.icons.filled.Remove // Simpler Remove icon
import androidx.compose.material.icons.filled.Star // For skill points
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Data class remains the same
private data class StatUpgradeItem(
    val type: StatType,
    val initialValue: Int,
    var pointsAdded: Int = 0
) {
    val currentValue: Int
        get() = initialValue + pointsAdded
}

// Enum with emojis (already good and child-friendly)
enum class StatType(val displayName: String) {
    HP("❤️ Health"),
    ATK("⚔️ Attack"),
    MP("✨ Magic"),
    DEF("🛡️ Defense"),
    STA("⚡ Stamina")
}

@Composable
fun StatUpgradeDialog(
    showDialog: Boolean,
    initialStats: Map<StatType, Int>,
    initialSkillPoints: Int = 4,
    onDismissRequest: () -> Unit,
    onConfirm: (upgradedStats: Map<StatType, Int>, pointsSpent: Int) -> Unit
) {
    if (!showDialog) return

    val statUpgradeItems = remember {
        StatType.values().mapNotNull { type ->
            initialStats[type]?.let { initialValue ->
                StatUpgradeItem(type, initialValue)
            } ?: StatUpgradeItem(type, 0) // Default if not in initialStats
        }.toMutableStateList()
    }

    var remainingSkillPoints by remember { mutableStateOf(initialSkillPoints) }

    LaunchedEffect(statUpgradeItems.map { it.pointsAdded }) {
        val pointsSpent = statUpgradeItems.sumOf { it.pointsAdded }
        remainingSkillPoints = initialSkillPoints - pointsSpent
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false // Important for custom width
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f) // Make dialog a bit narrower
                .wrapContentHeight(), // Adjust height to content
            shape = RoundedCornerShape(20.dp), // Still nicely rounded
            color = MaterialTheme.colorScheme.surface, // Standard surface color
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 20.dp), // Slightly reduced padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Boost Stats!", // Shorter title
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Skill Gems",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp) // Slightly smaller icon
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$remainingSkillPoints Gems Left", // Shorter
                        style = MaterialTheme.typography.titleMedium, // Adjusted size
                        fontWeight = FontWeight.Bold,
                        color = if (remainingSkillPoints > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Using a standard Column for the list of stats
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Tighter spacing for rows
                ) {
                    statUpgradeItems.forEachIndexed { index, item ->
                        SimplifiedStatRow(
                            statItem = item,
                            onIncrement = {
                                if (remainingSkillPoints > 0) {
                                    statUpgradeItems[index] = item.copy(pointsAdded = item.pointsAdded + 1)
                                }
                            },
                            onDecrement = {
                                if (item.pointsAdded > 0) {
                                    statUpgradeItems[index] = item.copy(pointsAdded = item.pointsAdded - 1)
                                }
                            },
                            canIncrement = remainingSkillPoints > 0,
                            canDecrement = item.pointsAdded > 0
                        )
                        if (index < statUpgradeItems.size - 1) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly, // Keep buttons spread
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton( // Using OutlinedButton for "Later" for less emphasis
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Later", fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val result = statUpgradeItems.associate { it.type to it.currentValue }
                            val pointsActuallySpent = statUpgradeItems.sumOf { it.pointsAdded } // Calculate here
                            onConfirm(result, pointsActuallySpent)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Power Up!", fontSize = 15.sp, fontWeight = FontWeight.Bold) // Shorter button text
                    }
                }
            }
        }
    }
}

@Composable
private fun SimplifiedStatRow(
    statItem: StatUpgradeItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean,
    canDecrement: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp), // Tighter vertical padding
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stat Info (Name and Value)
        Text(
            text = statItem.type.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1.8f) // Give more space to name
        )
        Text(
            text = "${statItem.currentValue}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f) // Space for value
        )
        if (statItem.pointsAdded > 0) {
            Text(
                text = "(+${statItem.pointsAdded})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF008000), // Green for added points
                modifier = Modifier.padding(start = 4.dp).weight(0.7f) // Space for added points
            )
        } else {
            Spacer(Modifier.weight(0.7f)) // Keep alignment if no points added
        }


        // Controls (+/- buttons)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1.5f) // Space for buttons
        ) {
            SmallIconButton(
                onClick = onDecrement,
                enabled = canDecrement,
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease ${statItem.type.displayName}"
            )
            Spacer(Modifier.width(4.dp)) // Small space between buttons
            SmallIconButton(
                onClick = onIncrement,
                enabled = canIncrement,
                icon = Icons.Filled.Add,
                contentDescription = "Increase ${statItem.type.displayName}"
            )
        }
    }
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(38.dp) // Slightly smaller icon buttons
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // Tint can be simpler, or use themed primary/secondary
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(22.dp) // Icon size within the button
        )
    }
}
