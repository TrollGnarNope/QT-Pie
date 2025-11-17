package com.veigar.questtracker.ui.component.notification

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.ui.component.child.QuizOutcome
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import java.util.concurrent.TimeUnit

fun Long.toTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
            // Fallback to a simple date format if older than a week
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(this)
        }
    }
}


@Composable
fun ChildFriendlyNotificationItem(
    notification: NotificationModel,
    onClick: () -> Unit,
    isParent: Boolean = false
) {
    Log.d("ChildFriendlyNotificationItem", "Notification: ${notification.clicked}")
    val iconDetails = getNotificationIconDetails(notification.category)
    
    // Use professional gray colors for parent users, colorful for children
    val baseColor = if (isParent) ProfessionalGray else CoralBlueDark
    val lightColor = if (isParent) ProfessionalGray.copy(alpha = 0.7f) else Color(0xFFD3E6F9)

    val cardBackgroundColor = if (notification.clicked) baseColor.copy(alpha = 0.85f) else baseColor
    val contentColor = if (isParent) ProfessionalGrayText else Color.White

    // Left edge indicator color
    val leftIndicatorColor = if (notification.clicked) baseColor.copy(alpha = 0.7f) else (if (isParent) ProfessionalGrayText else Color.White)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(2.dp), // Slightly less rounded for a more "blocky" feel
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.clicked) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .height(IntrinsicSize.Min), // Ensures Row elements can fill height if needed
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Edge Indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight() // Takes the height of the Row
                    .width(6.dp)
                    .background(leftIndicatorColor)
            )

            // Spacer after indicator
            Spacer(modifier = Modifier.width(10.dp))

            // 2. Icon
            Box(
                modifier = Modifier
                    .size(38.dp) // Icon container size
                    .clip(CircleShape) // Optional: if you want icon bg to be circle
                    .background(iconDetails.backgroundColor.copy(alpha = 0.2f)), // Subtle background for icon
                contentAlignment = Alignment.Center
            ) {
                if (iconDetails.painter != null) {
                    Image(
                        painter = iconDetails.painter,
                        contentDescription = notification.category.name,
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor) // Tint to contentColor (White)
                    )
                } else {
                    Icon(
                        imageVector = iconDetails.icon,
                        contentDescription = notification.category.name,
                        modifier = Modifier.size(22.dp),
                        tint = contentColor // Icon color (White)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 3. Vertical Column (Title, Description)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = if (notification.clicked) FontWeight.Normal else FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (notification.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = contentColor.copy(alpha = 0.85f), // Slightly less prominent
                        maxLines = 2, // Keep it very concise
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Timestamp
            Text(
                text = notification.timestamp.toTimeAgo(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = contentColor.copy(alpha = 0.7f), // Subtle timestamp
                modifier = Modifier.padding(start = 8.dp, end = 12.dp) // Padding for timestamp
            )
        }
    }
}

// Re-using the same NotificationIconDetails data class
data class NotificationIconDetails(
    val icon: ImageVector,
    val painter: Painter? = null,
    val iconColor: Color, // This is now more of a "base" category color
    val backgroundColor: Color // Used for subtle icon background disc
)

@Composable
fun getNotificationIconDetails(category: NotificationCategory): NotificationIconDetails {
    // These colors will primarily be used for the small disc behind the main icon.
    // The main icon itself will be white.
    return when (category) {
        NotificationCategory.TASK_CHANGE -> NotificationIconDetails(
            icon = Icons.Rounded.CheckCircleOutline,
            iconColor = Color(0xFF4CAF50), // Green (base category color)
            backgroundColor = Color.White // Background for the icon disc
        )
        NotificationCategory.LOCATION_UPDATE -> NotificationIconDetails(
            icon = Icons.Rounded.LocationOn,
            iconColor = Color(0xFF2196F3), // Blue
            backgroundColor = Color.White
        )
        NotificationCategory.REWARD -> NotificationIconDetails(
            icon = Icons.Rounded.Star,
            iconColor = Color(0xFFFF9800), // Orange/Amber
            backgroundColor = Color.White
        )
        NotificationCategory.SYSTEM -> NotificationIconDetails(
            icon = Icons.Rounded.Info,
            iconColor = Color(0xFF78909C), // Blue Grey
            backgroundColor = Color.White
        )
        NotificationCategory.LOCATION_REQUEST, NotificationCategory.UNKNOWN, NotificationCategory.OTHER -> NotificationIconDetails(
            icon = Icons.Rounded.Notifications,
            iconColor = Color(0xFF9E9E9E), // Grey
            backgroundColor = Color.White
        )
    }
}

/**
 * Creates a notification for quiz outcomes (rewards or punishments)
 */
fun createQuizOutcomeNotification(outcome: QuizOutcome): NotificationModel {
    val title = when {
        outcome.isOverdue -> "Quiz Overdue Penalty"
        outcome.isRewarded -> "Quiz Reward Earned!"
        else -> "Quiz Penalty Applied"
    }
    
    val message = when {
        outcome.isOverdue -> {
            "You received a penalty for submitting '${outcome.quizTitle}' after the deadline. " +
            "Points: ${outcome.pointsChange}"
        }
        outcome.isRewarded -> {
            "Great job on '${outcome.quizTitle}'! You scored ${outcome.score}% and earned rewards. " +
            "Points: +${outcome.pointsChange}"
        }
        else -> {
            "You received a penalty for '${outcome.quizTitle}' (Score: ${outcome.score}%). " +
            "Points: ${outcome.pointsChange}"
        }
    }
    
    return NotificationModel(
        title = title,
        message = message,
        timestamp = System.currentTimeMillis(),
        category = NotificationCategory.REWARD, // Using REWARD category for all quiz outcomes
        clicked = false
    )
}