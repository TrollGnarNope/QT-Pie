package com.veigar.questtracker.ui.component.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.RewardsModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestQuestDialog(
    onDismiss: () -> Unit,
    onRequest: (String, String, RewardsModel?, String?) -> Unit // Updated to include RewardsModel and icon
) {
    var questName by remember { mutableStateOf("") }
    var questDescription by remember { mutableStateOf("") }
    // Removed xpReward and coinReward states
    // Removed iconName state

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White, // Changed to Color.White
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF90CAF9)))) // Light blue gradient
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Request A Quest",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                OutlinedTextField(
                    value = questName,
                    onValueChange = { questName = it },
                    label = { Text("Quest Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = questDescription,
                    onValueChange = { questDescription = it },
                    label = { Text("Quest Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Removed "Proposed Rewards (Optional)" Text and associated Row with OutlinedTextFields

                // Removed OutlinedTextField for iconName as rewards are removed.
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Rewards and icon are no longer passed from here, pass null for now.
                    onRequest(questName, questDescription, null, null)
                },
                enabled = questName.isNotBlank() && questDescription.isNotBlank()
            ) {
                Text("Request Quest")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
