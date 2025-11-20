package com.veigar.questtracker.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.theme.CoralBlueDarkest
import com.veigar.questtracker.ui.theme.yellow

@Composable
fun ChildChip(
    userModel: UserModel,
    isSelected: Boolean,
    subtitle: String? = null,
    showWishlistIndicator: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(
                color = if (isSelected) yellow else CoralBlueDarkest,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Box {
            DisplayAvatar(
                fullAssetPath = userModel.avatarUrl,
                size = 36.dp
            )
            if(showWishlistIndicator) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Red, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            AnimatedVisibility(
                visible = isSelected || subtitle != null,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = userModel.name,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}