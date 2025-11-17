package com.veigar.questtracker.ui.component.chats

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.veigar.questtracker.model.MessagesModel
import com.veigar.questtracker.ui.component.DisplayAvatar
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatList(
    messages: List<MessagesModel>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No messages yet. Start chatting!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 8.dp),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageItem(
                    message = message,
                    isCurrentUserMessage = message.senderId == currentUserId,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                )
            }
        }
    }
}


@Composable
fun MessageItem(
    message: MessagesModel,
    isCurrentUserMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isCurrentUserMessage) Alignment.End else Alignment.Start
    val bubbleGradient = if (isCurrentUserMessage) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF007BFF), Color(0xFF00C6FF)) // Blue gradient
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF4DB6AC), Color(0xFF80CBC4))
        )
    }
    val textColor = Color.White

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxWidthPercentage = 0.7f // 70% of screen width


    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!isCurrentUserMessage && message.senderAvatar?.isNotBlank() == true) {
                DisplayAvatar(
                    fullAssetPath = message.senderAvatar,
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier
                    .widthIn(max = screenWidth * maxWidthPercentage)
                    .background(
                        brush = bubbleGradient,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(
                        start = if (isCurrentUserMessage || message.senderAvatar?.isNotBlank() != true) 0.dp else 0.dp, // No extra start padding if avatar is there
                        end = if (!isCurrentUserMessage || message.senderAvatar?.isNotBlank() != true) 0.dp else 0.dp
                    ),
            ) {
                Column(modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )) {
                    if (message.message?.isNotBlank() == true) {
                        Text(
                            text = message.message!!,
                            color = textColor,
                            fontSize = 16.sp
                        )
                    }
                    if (message.img?.isNotBlank() == true) {
                        // Display image if present
                        AsyncImage(
                            model = message.img,
                            contentDescription = "Chat image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (message.dateTime?.isNotBlank() == true) {
                        Text(
                            text = formatDateTime(message.dateTime), // Helper function for formatting
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            if (isCurrentUserMessage && message.senderAvatar?.isNotBlank() == true) {
                Spacer(modifier = Modifier.width(8.dp))
                DisplayAvatar(
                    fullAssetPath = message.senderAvatar,
                    size = 32.dp,
                )
            }
        }
        // Optionally, display "seen by" info if needed
        if (message.seenBy?.isNotEmpty() == true && isCurrentUserMessage) {
            Text(
                text = "Seen by ${message.seenBy?.joinToString()}", // Simple representation
                fontSize = 10.sp,
                color = Color.LightGray, // Adjust color as needed
                modifier = Modifier.padding(
                    top = 2.dp,
                    end = if (message.senderAvatar?.isNotBlank() == true) 40.dp else 8.dp
                ) // Align under bubble
            )
        }
    }
}

// Helper function to format dateTime string (assuming it's a timestamp in millis as String)
fun formatDateTime(dateTimeString: String?): String {
    if (dateTimeString.isNullOrBlank()) return ""
    return try {
        val timestamp = dateTimeString.toLong()
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Example: 10:30 AM
        sdf.format(java.util.Date(timestamp))
    } catch (e: NumberFormatException) {
        dateTimeString // Return original if not a valid timestamp
    } catch (e: Exception) {
        "Invalid date"
    }
}
