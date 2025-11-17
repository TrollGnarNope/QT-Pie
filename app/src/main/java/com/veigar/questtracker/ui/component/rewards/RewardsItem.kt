package com.veigar.questtracker.ui.component.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info // For general info tags if needed
import androidx.compose.material.icons.filled.Star // For points chip
import androidx.compose.material.icons.rounded.Approval // For approval tag
import androidx.compose.material.icons.filled.Warning // For low quantity warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.RedemptionStatus
import com.veigar.questtracker.model.RewardModel
val ParentCardBackground = Color(0xFFE0F7FA)
val ParentCardAccent = Color(0xFFFFCA28)
val ParentTextColor = Color(0xFF37474F)
val ParentMutedTextColor = Color(0xFF78909C)
val ParentHighlightColor = Color(0xFFE57373)
val ParentQuantityTagColor = Color(0xFF4FC3F7)
val ParentActionIconColor = Color(0xFF546E7A)
val ParentPointsColor = Color(0xFFFF8A65)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardItem(
    reward: RewardModel,
    onEditClicked: (RewardModel) -> Unit,
    onDeleteClicked: (RewardModel) -> Unit,
    onHistoryClicked: (RewardModel) -> Unit,
    modifier: Modifier = Modifier,
    isParent: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClick = { onHistoryClicked(reward) }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, hoveredElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = if (isParent) ProfessionalGray else ParentCardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = reward.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isParent) ProfessionalGrayText else ParentTextColor,
                        fontSize = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                ParentChipForPoints(points = reward.pointsRequired)
            }

            // Optional Description
            if (!reward.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isParent) ProfessionalGrayText.copy(alpha = 0.8f) else ParentMutedTextColor,
                        fontSize = 14.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reward.requiresApproval) {
                        ParentInfoTag(
                            icon = Icons.Rounded.Approval,
                            backgroundColor = ParentCardAccent.copy(alpha = 0.2f),
                            contentColor = if (isParent) ProfessionalGrayText.copy(alpha = 0.9f) else ParentTextColor.copy(alpha = 0.9f),
                            iconOnly = true, // Icon only for "needs approval" seems concise
                            contentDescription = "Accept/Reject"
                        )
                    }


                    if (reward.quantityLimit != null) {
                        val redeemed = reward.redemptionHistory.filter {
                            it.status == RedemptionStatus.REDEEMED || it.status == RedemptionStatus.APPROVED
                        }
                        val available = reward.quantityLimit.minus(redeemed.size)
                        if (available <= 5) {
                            InfoTag(
                                text = "$available left!",
                                icon = Icons.Filled.Warning,
                                backgroundColor = ParentHighlightColor.copy(alpha = 0.15f),
                                contentColor = ParentHighlightColor,
                                iconOnly = false
                            )
                        } else {
                            InfoTag(
                                text = "$available available",
                                icon = Icons.Filled.Info,
                                backgroundColor = ParentQuantityTagColor.copy(alpha = 0.2f),
                                contentColor = ParentQuantityTagColor,
                                iconOnly = false
                            )
                        }
                    }
                    val needsApproval = reward.redemptionHistory.any { it.status == RedemptionStatus.PENDING_APPROVAL }
                    if(needsApproval){
                        InfoTag(
                            text = "Accept/Reject",
                            icon = Icons.Filled.Warning,
                            backgroundColor = ParentHighlightColor.copy(alpha = 0.15f),
                            contentColor = ParentHighlightColor,
                            iconOnly = false
                        )
                    } else {
                        InfoTag(
                            text = "${reward.redemptionHistory.size} record",
                            icon = Icons.Filled.Info,
                            backgroundColor = ParentQuantityTagColor.copy(alpha = 0.2f),
                            contentColor = ParentQuantityTagColor,
                            iconOnly = false
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            
            // Action buttons with better spacing and visual hierarchy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                IconButton(
                    onClick = { onEditClicked(reward) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isParent) ProfessionalGrayText.copy(alpha = 0.1f) else ParentActionIconColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Reward",
                        tint = if (isParent) ProfessionalGrayText else ParentActionIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
                // Delete button
                IconButton(
                    onClick = { onDeleteClicked(reward) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isParent) ParentHighlightColor.copy(alpha = 0.1f) else ParentHighlightColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Reward",
                        tint = if (isParent) ParentHighlightColor else ParentHighlightColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ParentChipForPoints(points: Int) {
    Row(
        modifier = Modifier
            .background(ParentPointsColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = "Points",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$points",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ParentInfoTag(
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
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
