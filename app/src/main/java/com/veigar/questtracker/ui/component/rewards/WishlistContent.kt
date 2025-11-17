package com.veigar.questtracker.ui.component.rewards

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.ChildWishListItem
import com.veigar.questtracker.model.RedemptionStatus

@Composable
fun WishlistContent(
    modifier: Modifier = Modifier,
    wishList: List<ChildWishListItem>,
    onApproveClicked: (ChildWishListItem) -> Unit,
    onDeclineClicked: (ChildWishListItem) -> Unit,
    isParent: Boolean = false
){
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        items(wishList.size){
            WishListItemCardForParent(
                item = wishList[it],
                onApproveClicked = onApproveClicked,
                onDeclineClicked = onDeclineClicked,
                isParent = isParent
            )
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
fun WishListItemCardForParent(
    item: ChildWishListItem,
    onApproveClicked: (ChildWishListItem) -> Unit,
    onDeclineClicked: (ChildWishListItem) -> Unit,
    modifier: Modifier = Modifier,
    isParent: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isParent) ProfessionalGray else ChildCardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        color = if (isParent) ProfessionalGrayText else ChildTextColor,
                        fontSize = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Action buttons with better visual feedback
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Approve Button
                    IconButton(
                        onClick = { onApproveClicked(item) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (item.status == RedemptionStatus.APPROVED) Color.Green.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Approve",
                            tint = if (item.status == RedemptionStatus.APPROVED) Color.Green else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Decline Button
                    IconButton(
                        onClick = { onDeclineClicked(item) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (item.status == RedemptionStatus.DECLINED) Color.Red.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = "Decline",
                            tint = if (item.status == RedemptionStatus.DECLINED) Color.Red else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }




            }

            // Optional Description
            if (!item.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isParent) ProfessionalGrayText.copy(alpha = 0.8f) else ChildMutedTextColor,
                        fontSize = 14.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

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