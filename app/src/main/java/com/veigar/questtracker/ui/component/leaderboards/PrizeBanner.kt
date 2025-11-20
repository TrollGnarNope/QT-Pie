package com.veigar.questtracker.ui.component.leaderboards

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.veigar.questtracker.R
import com.veigar.questtracker.ui.theme.DailyQuestGradientEnd
import com.veigar.questtracker.ui.theme.DailyQuestGradientStart

@Composable
fun PrizeBanner(
    title: String,
    prizeText: String,
    iconUrl: String?,
    endsInText: String = "Ends in ???"
) {
    // Use reusable gradients and color definitions
    val GradientStart = DailyQuestGradientStart
    val GradientEnd = DailyQuestGradientEnd
    val TitleColor = Color(0xFFFFFFFF)
    val PrizeTextColor = Color.White
    val EndsInColor = Color(0xFF818181)   // Soft gray-blue
    val ShadowColor = Color.Black.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors = listOf(GradientStart, GradientEnd)))
                .clip(RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column for Title, PrizeText and EndsInText
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    PrizeBannerTitle(title = title, titleColor = TitleColor, shadowColor = ShadowColor)
                    PrizeBannerPrizeText(prizeText = prizeText, prizeTextColor = PrizeTextColor, shadowColor = ShadowColor)
                    PrizeBannerEndsInText(endsInText = endsInText, endsInColor = EndsInColor)
                }

                // AsyncImage for Icon
                PrizeBannerIcon(iconUrl = iconUrl)
            }
        }
    }
}

@Composable
fun PrizeBannerTitle(title: String, titleColor: Color, shadowColor: Color) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = titleColor,
            shadow = Shadow(
                shadowColor,
                offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                blurRadius = 6f
            )
        )
    )
}

@Composable
fun PrizeBannerPrizeText(prizeText: String, prizeTextColor: Color, shadowColor: Color) {
    Text(
        text = prizeText,
        style = TextStyle(
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = prizeTextColor,
            shadow = Shadow(
                shadowColor,
                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                blurRadius = 4f
            )
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun PrizeBannerEndsInText(endsInText: String, endsInColor: Color) {
    Text(
        text = endsInText,
        fontSize = 14.sp,
        color = endsInColor,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
fun PrizeBannerIcon(iconUrl: String?) {
    AsyncImage(
        model = iconUrl,
        contentDescription = "Icon",
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.trophy),
        error = painterResource(id = R.drawable.trophy),
        fallback = painterResource(id = R.drawable.trophy),
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
    )
}