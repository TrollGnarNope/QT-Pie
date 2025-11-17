package com.veigar.questtracker.ui.component.rewards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.copy
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.RedemptionRecord
import com.veigar.questtracker.model.RedemptionStatus
import com.veigar.questtracker.model.RewardModel
import java.text.SimpleDateFormat
import java.util.Locale

val SheetBackgroundColor = Color(0xFFF8F9FA) // Light grey
val SheetHeaderColor = Color(0xFF4A4A4A)     // Dark Grey for header text
val SheetItemPrimaryTextColor = Color(0xFF333333)
val SheetItemSecondaryTextColor = Color(0xFF666666)
val StatusPendingColor = Color(0xFFFFA000)
val StatusRedeemedColor = Color(0xFF388E3C)
val StatusDeniedColor = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionHistorySheet(
    reward: RewardModel,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onApproveRedemption: (redemptionId: String) -> Unit,
    onDeclineRedemption: (redemptionId: String) -> Unit
) {
    val redemptionHistory = reward.redemptionHistory
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackgroundColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp) // Padding for content below the list
        ) {
            Text(
                text = "Redemption History",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SheetHeaderColor,
                    fontSize = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )

            if (redemptionHistory.isEmpty()) {
                Text(
                    text = "No one has redeemed or requested this reward yet.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SheetItemSecondaryTextColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp) // Padding at the end of the list
                ) {
                    items(redemptionHistory.sortedByDescending { it.redeemedAt }) { redemption ->
                        RedemptionHistoryItem(
                            redemption = redemption,
                            onApproveClicked = { onApproveRedemption(redemption.redemptionId) },
                            onDeclineClicked = { onDeclineRedemption(redemption.redemptionId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RedemptionHistoryItem(
    redemption: RedemptionRecord,
    onApproveClicked: () -> Unit,
    onDeclineClicked: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat(
            "MMM dd, yyyy 'at' hh:mm a",
            Locale.getDefault()
        )
    }

    val formattedDate = remember(redemption.status, redemption.approvalTimestamp, redemption.redeemedAt) {
        val dateToFormat = if (redemption.status == RedemptionStatus.APPROVED && redemption.approvalTimestamp != null) {
            redemption.approvalTimestamp
        } else {
            redemption.redeemedAt
        }
        dateFormatter.format(
            java.util.Date(
                dateToFormat
            ))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(status = redemption.status)

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = redemption.child?.name ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SheetItemPrimaryTextColor,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "Status: ${redemption.status.name.replace("_", " ").lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = getStatusColor(redemption.status).copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Points: ${redemption.pointsSpent}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SheetItemSecondaryTextColor,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SheetItemSecondaryTextColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            AnimatedVisibility(
                visible = redemption.status == RedemptionStatus.PENDING_APPROVAL,
                enter = fadeIn(), // Optional: add animation
                exit = fadeOut()  // Optional: add animation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp), // Padding for the button row
                    horizontalArrangement = Arrangement.End, // Align buttons to the right
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton( // Using OutlinedButton for a less prominent look than FilledButton
                        onClick = onDeclineClicked,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDeniedColor),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Decline", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Decline", fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onApproveClicked,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusRedeemedColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Done, contentDescription = "Approve", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Approve", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: RedemptionStatus) {
    val (icon, color, contentDesc) = when (status) {
        RedemptionStatus.PENDING_APPROVAL -> Triple(Icons.Filled.HourglassEmpty, StatusPendingColor, "Pending Approval")
        RedemptionStatus.REDEEMED -> Triple(Icons.Filled.CheckCircle, StatusRedeemedColor, "Redeemed")
        RedemptionStatus.APPROVED -> Triple(Icons.Filled.CheckCircle, StatusRedeemedColor, "Approved")
        RedemptionStatus.DECLINED -> Triple(Icons.Filled.Cancel, StatusDeniedColor, "Denied")
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = color,
            modifier = Modifier.fillMaxSize()
        )
    }
}

fun getStatusColor(status: RedemptionStatus): Color {
    return when (status) {
        RedemptionStatus.PENDING_APPROVAL -> StatusPendingColor
        RedemptionStatus.REDEEMED -> StatusRedeemedColor
        RedemptionStatus.APPROVED -> StatusRedeemedColor
        RedemptionStatus.DECLINED -> StatusDeniedColor
    }
}