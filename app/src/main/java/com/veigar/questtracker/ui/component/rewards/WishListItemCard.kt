package com.veigar.questtracker.ui.component.rewards

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.ChildWishListItem
import com.veigar.questtracker.model.RedemptionStatus

@SuppressLint("SimpleDateFormat")
@Composable
fun WishListItemCard(
    item: ChildWishListItem,
    onClick: (ChildWishListItem) -> Unit,
    onDelete: (ChildWishListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(item) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = ChildCardBackground)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ChildTextColor,
                        fontSize = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Delete Icon
                IconButton(
                    onClick = { onDelete(item) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }

            // Optional Description
            if (!item.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ChildMutedTextColor,
                        fontSize = 13.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            // Status + Optional Approval Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.status != RedemptionStatus.PENDING_APPROVAL) {
                    val (icon, color) = when (item.status) {
                        RedemptionStatus.REDEEMED -> Icons.Filled.Redeem to ChildCardAccent
                        RedemptionStatus.APPROVED -> Icons.Filled.Approval to QuantityTagColor
                        RedemptionStatus.DECLINED -> Icons.Filled.Info to ChildHighlightColor
                        else -> Icons.Filled.Info to ChildHighlightColor // Should not happen
                    }

                    InfoTag(
                        icon = icon,
                        backgroundColor = color.copy(alpha = 0.2f),
                        contentColor = color,
                        iconOnly = false,
                        text = item.status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    )
                }

                item.approvalTimestamp?.let {
                    Text(
                        text = "Approved: ${java.text.SimpleDateFormat("dd MMM yyyy").format(it)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ChildMutedTextColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

