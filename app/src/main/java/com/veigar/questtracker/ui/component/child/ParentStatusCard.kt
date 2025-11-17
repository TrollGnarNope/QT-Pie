package com.veigar.questtracker.ui.component.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.DisplayAvatar

@Composable
fun ParentLinkStatusCard(
    modifier: Modifier = Modifier,
    parentUserModel: UserModel?,
    parentActiveStatus: Boolean?,
    onLinkParentClicked: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)

    // Fixed colors for parent status card
    val gradientColors = listOf(
        Color(0xFF2C3E50), // Professional dark blue-gray
        Color(0xFF34495E)  // Slightly lighter blue-gray
    )

    val cardGradientBrush = Brush.horizontalGradient(colors = gradientColors)
    val contentColor = Color.White // Fixed white text for good contrast

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Add elevation
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent, // Card itself is transparent
            contentColor = contentColor         // Default content color for children
        )
    ) {
        // This Column will hold the gradient and the content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = cardGradientBrush, shape = cardShape) // Apply gradient here
                .clip(cardShape) // Ensure content respects card's shape, especially with padding
                .padding(horizontal = 16.dp, vertical = 12.dp), // Adjusted padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (parentUserModel != null) {
                // --- Parent is Linked ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing
                ) {
                    DisplayAvatar(
                        fullAssetPath = parentUserModel.avatarUrl,
                        size = 40.dp // Slightly larger avatar
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked Parent", // Simplified text
                            style = MaterialTheme.typography.labelMedium, // Adjusted style
                            color = contentColor.copy(alpha = 0.85f)
                        )
                        Text(
                            text = parentUserModel.name,
                            style = MaterialTheme.typography.titleMedium.copy( // Adjusted style
                                fontWeight = FontWeight.Bold
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Removed Spacer, relying on Column's padding

            } else {
                // --- No Parent Linked ---
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Link to Parent",
                    tint = contentColor.copy(alpha = 0.9f), // Use content color
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Link with Your Parent!",
                    style = MaterialTheme.typography.headlineSmall.copy( // More prominent
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connect to see your quests and progress.", // More informative
                    style = MaterialTheme.typography.bodyMedium, // Adjusted style
                    color = contentColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp)) // Increased spacing
                Button(
                    onClick = onLinkParentClicked,
                    shape = RoundedCornerShape(20.dp), // More rounded button
                    colors = ButtonDefaults.buttonColors(
                        // Make button stand out - consider a semi-transparent version of contentColor's opposite
                        containerColor = if (contentColor == Color.White) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.3f),
                        contentColor = contentColor
                    ),
                    modifier = Modifier.height(48.dp).fillMaxWidth(0.7f) // Taller and wider button
                ) {
                    Icon(
                        Icons.Filled.Link,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Link Up Now", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

