package com.veigar.questtracker.ui.component.child

import android.icu.util.Calendar
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.UserModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChildGreetingCard(userModel: UserModel?, modifier: Modifier = Modifier) {

    data class GreetingInfo(
        val text: String,
        val icon: ImageVector,
        val iconColor: Color,
        val backgroundGradient: List<Color>
    )

    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

    val greetingInfo = when (currentHour) {
        in 0..5 -> GreetingInfo( // Late Night
            text = "Good Night",
            icon = Icons.Filled.NightsStay,
            iconColor = Color(0xFFD1C4E9),
            backgroundGradient = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)) // Very dark blue night
        )
        in 6..11 -> GreetingInfo( // Morning
            text = "Good Morning",
            icon = Icons.Filled.WbSunny,
            iconColor = Color(0xFFFFF176),
            backgroundGradient = listOf(Color(0xFF01579B), Color(0xFF0277BD)) // Deep sky blue
        )
        in 12..17 -> GreetingInfo( // Afternoon
            text = "Good Afternoon",
            icon = Icons.Filled.WbSunny,
            iconColor = Color(0xFFFFF176),
            backgroundGradient = listOf(Color(0xFFFF9949), Color(0xFFFF7B3C)) // Burnt orange to deep amber
        )
        else -> GreetingInfo( // Evening
            text = "Good Evening",
            icon = Icons.Filled.NightsStay,
            iconColor = Color(0xFFB3E5FC),
            backgroundGradient = listOf(Color(0xFF263238), Color(0xFF37474F)) // Charcoal to dark steel
        )
    }

    val textColorPrimary = Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(greetingInfo.backgroundGradient)
            )
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Slightly more space
        ) {
            Icon(
                imageVector = greetingInfo.icon,
                contentDescription = greetingInfo.text,
                tint = greetingInfo.iconColor,
                modifier = Modifier.size(32.dp) // Slightly larger icon
            )
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${greetingInfo.text}, ${userModel?.name}!",
                    style = MaterialTheme.typography.headlineSmall.copy( // headlineSmall is fitting
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary,
                        fontSize = 20.sp // Child-friendly size
                    ),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (currentHour) {
                in 6..11 -> "Ready for today's adventures? Let's go! 🚀"
                in 12..17 -> "Hope you're having a super day! What's next? 🤔"
                else -> "Time to wind down or get set for new quests! ✨"
            },
            style = MaterialTheme.typography.bodyMedium.copy( // bodyMedium is smaller than bodyLarge
                color = Color.White,
                lineHeight = 20.sp // Adjust line height for smaller font
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}