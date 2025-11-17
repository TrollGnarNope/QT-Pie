package com.veigar.questtracker.ui.component.createtask

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateTaskButton(
    modifier: Modifier = Modifier, // Allow parent to pass modifiers
    onClick: () -> Unit,
    enabled: Boolean = true // Optional: to disable the button e.g. while saving
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier // Apply passed modifiers (e.g., fillMaxWidth, padding)
            .fillMaxWidth() // Default to fill width as per mockup
            .padding(vertical = 8.dp), // Add some default vertical padding
        shape = MaterialTheme.shapes.medium, // Or use specific corner radius
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(vertical = 14.dp) // Generous padding for a primary action button
    ) {
        Text(
            text = "CREATE TASK",
            style = MaterialTheme.typography.titleMedium, // Or a custom style for buttons
            fontSize = 16.sp // Slightly larger font for a main button
        )
    }
}