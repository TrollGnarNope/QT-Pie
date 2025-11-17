package com.veigar.questtracker.ui.component.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.veigar.questtracker.model.RewardModel

// Child-friendly Dialog Colors
val DialogBackgroundSoft = Color(0xFFF8F9FA) // Very light, almost white
val DialogAccentPositive = Color(0xFF76D7C4) // Soft teal for "Yes"
val DialogAccentNegative = Color(0xFFF5B7B1) // Soft pink for "No"
val DialogTextColor = Color(0xFF34495E)       // Dark, friendly blue-grey
val DialogMutedTextColor = Color(0xFF5D6D7E)  // Lighter blue-grey
val DialogTitleColor = Color(0xFF2E86C1)      // Friendly blue for title

@Composable
fun ClaimRewardDialog(
    reward: RewardModel,
    onConfirm: (RewardModel) -> Unit,
    onDismiss: () -> Unit,
    currentChildPoints: Int // To potentially show remaining points or if they can't afford
) {
    // Determine if child can actually afford, though this dialog
    // should ideally only be shown if they can. This is a safeguard.
    val canAfford = currentChildPoints >= reward.pointsRequired

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp), // Softer corners
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBackgroundSoft)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Optional: Icon for celebration/reward
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = DialogAccentPositive,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = reward.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DialogTitleColor,
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp
                    )
                )

                // Points Cost
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Points",
                        tint = ChildCardAccent, // Use accent from your card
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${reward.pointsRequired} points",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = DialogTextColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    )
                }

                Text(
                    text = "Do you want to get this amazing reward?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = DialogMutedTextColor,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    // "Not Yet" Button
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DialogAccentNegative.copy(alpha = 0.8f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Not Yet", fontWeight = FontWeight.Bold)
                    }

                    // "Yes, Get it!" Button
                    Button(
                        onClick = { onConfirm(reward) },
                        enabled = canAfford, // Should always be true if dialog is shown correctly
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DialogAccentPositive,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Get it!", fontWeight = FontWeight.Bold)
                    }
                }

                if (!canAfford) {
                    Text(
                        text = "Oops! You don't have enough points for this yet.",
                        color = ChildHighlightColor, // From your card theme
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
