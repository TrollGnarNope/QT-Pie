package com.veigar.questtracker.ui.component.rewards

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.RewardModel

// Define some soft, child-friendly colors for the dialog
val DialogSoftBackground = Color(0xFFFFF9E0) // Very light yellow, like parchment
val DialogPrimaryText = Color(0xFF5D4037)  // Soft Brown
val DialogSecondaryText = Color(0xFF795548) // Lighter Soft Brown
val DialogConfirmButtonSoft = Color(0xFFFBC02D) // Softer Yellow for confirm (less alarming than red)
val DialogConfirmText = Color(0xFF4E342E)   // Dark brown text for good contrast on yellow
val DialogCancelButtonSoft = Color(0xFFBCAAA4) // Soft Beige/Grey for cancel
val DialogCancelText = Color(0xFF6D4C41)   // Brown text for cancel

@OptIn(ExperimentalMaterial3Api::class) // For AlertDialog
@Composable
fun DeleteRewardDialog(
    rewardToDelete: RewardModel,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp), // Softer, more rounded dialog
        containerColor = DialogSoftBackground, // Soft background color
        icon = {
            Image(
                imageVector = Icons.Outlined.HelpOutline, // A questioning icon
                contentDescription = "Confirmation",
                colorFilter = ColorFilter.tint(DialogPrimaryText),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Wait a Second!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = DialogPrimaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Are you sure you want to say goodbye to the",
                    fontSize = 15.sp,
                    color = DialogSecondaryText,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "'${rewardToDelete.title}' reward?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = DialogPrimaryText, // Emphasize the reward title slightly
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                Text(
                    text = "(Even the records are deleted!)", // Softer way to say "cannot be undone"
                    fontSize = 13.sp,
                    color = DialogSecondaryText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete()
                    onDismissRequest()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DialogConfirmButtonSoft,
                    contentColor = DialogConfirmText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp) // Give button some breathing room
            ) {
                Icon(
                    Icons.Filled.DeleteSweep, // A "sweeping away" icon, less harsh than a trash can
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Yes, I'm Sure", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton( // Using TextButton for a less prominent cancel
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp) // Give button some breathing room
            ) {
                Text("No, Keep It!", color = DialogCancelText, fontWeight = FontWeight.SemiBold)
            }
        },
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp) // Overall dialog padding
    )
}

