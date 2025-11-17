package com.veigar.questtracker.ui.component.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.RedemptionStatus
import com.veigar.questtracker.model.RewardModel

val ChildCardBackground = Color(0xFFE3F2FD)
val ChildCardAccent = Color(0xFFFFD54F)
val ChildTextColor = Color(0xFF37474F)
val ChildMutedTextColor = Color(0xFF78909C)
val ChildHighlightColor = Color(0xFFF06292)
val QuantityTagColor = Color(0xFF4FC3F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardCardForChild(
    reward: RewardModel,
    onClaimClicked: (RewardModel) -> Unit,
    onHistoryClicked: (RewardModel) -> Unit,
    canAfford: Boolean,
    childId: String
) {
    val rewardHistory = reward.redemptionHistory
    val isRedeemed = rewardHistory.any { it.child?.getDecodedUid() == childId && it.status == RedemptionStatus.REDEEMED }
    val isOnApproval = rewardHistory.any { it.child?.getDecodedUid() == childId && it.status == RedemptionStatus.PENDING_APPROVAL }
    // if quantityLimit is null then its false, means its one stock per child
    var noStocksAvailable = false
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = { onHistoryClicked(reward) }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = ChildCardBackground)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Title and Points Chip Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reward.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ChildTextColor,
                        fontSize = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                ChipForPoints(points = reward.pointsRequired)
            }

            // Optional Description
            if (!reward.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = ChildMutedTextColor, fontSize = 13.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Spacer before the final row (tags and button)
            // Adjust this spacer if description is absent or present for consistent spacing
            Spacer(Modifier.height(if (reward.description.isNullOrBlank()) 10.dp else 8.dp))

            // --- COMBINED ROW FOR TAGS AND BUTTON ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reward.requiresApproval) {
                        InfoTag(
                            icon = Icons.Filled.Approval,
                            backgroundColor = ChildCardAccent.copy(alpha = 0.2f),
                            contentColor = ChildTextColor.copy(alpha = 0.9f),
                            iconOnly = true,
                            contentDescription = "Accept/Reject"
                        )
                    }

                    // 2. Quantity Limit Tag (if there's a limit > 0)
                    if (reward.quantityLimit != null) {
                        val redeemed = reward.redemptionHistory.filter {
                            it.status == RedemptionStatus.REDEEMED || it.status == RedemptionStatus.APPROVED
                        }
                        val available = reward.quantityLimit.minus(redeemed.size)
                        noStocksAvailable = available == 0
                        if (available <= 5) {
                            InfoTag(
                                text = "$available left!",
                                icon = Icons.Filled.Warning,
                                backgroundColor = ChildHighlightColor.copy(alpha = 0.15f),
                                contentColor = ChildHighlightColor,
                                iconOnly = false
                            )
                        } else {
                            InfoTag(
                                text = "$available available",
                                icon = Icons.Filled.Info,
                                backgroundColor = QuantityTagColor.copy(alpha = 0.2f),
                                contentColor = QuantityTagColor,
                                iconOnly = false
                            )
                        }
                    }
                }

                // Flexible Spacer to push button to the end
                Spacer(Modifier.weight(1f))

                // Claim Button

                Button(
                    onClick = { if(!noStocksAvailable)onClaimClicked(reward) },
                    enabled = !isOnApproval && !isRedeemed && canAfford, // No stocks check handled by text
                    modifier = Modifier
                        .height(38.dp), // Slightly smaller button to fit better on one line with tags
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp), // Adjust padding
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) ChildCardAccent else ChildMutedTextColor.copy(alpha = 0.5f),
                        contentColor = ChildTextColor,
                        disabledContainerColor = if (noStocksAvailable) ChildMutedTextColor.copy(alpha = 0.4f) // Specific disabled color for no stocks
                                                else if (isRedeemed) ChildMutedTextColor.copy(alpha = 0.6f)
                                                else ChildMutedTextColor.copy(alpha = 0.3f),
                        disabledContentColor = ChildTextColor.copy(alpha = 0.7f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        Icons.Filled.Redeem,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = if (isOnApproval) "WAITING"
                        else if (isRedeemed) "REDEEMED"
                        else if (noStocksAvailable) "All Gone!"
                        else if (!canAfford) "More Points!"
                        else "Get it!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp // Slightly smaller text for the button
                    )
                }
            }
        }
    }
}

@Composable
fun ChipForPoints(points: Int) {
    Row(
        modifier = Modifier
            .background(ChildCardAccent, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = "Points",
            tint = ChildTextColor.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "$points",
            fontWeight = FontWeight.Bold,
            color = ChildTextColor,
            fontSize = 13.sp
        )
    }
}

@Composable
fun InfoTag(
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    iconOnly: Boolean,
    text: String? = null,
    contentDescription: String? = null
) {
    Row(
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(6.dp))
            .padding(
                horizontal = if (iconOnly) 4.dp else 6.dp,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription ?: text,
            tint = contentColor,
            modifier = Modifier.size(if (iconOnly) 18.dp else 14.dp)
        )
        if (!iconOnly && text != null) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
