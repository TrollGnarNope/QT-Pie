package com.veigar.questtracker.ui.component.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildRedemptionHistorySheet(
    reward: RewardModel,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        ChildRedemptionHistoryItem(
                            redemption = redemption
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChildRedemptionHistoryItem(
    redemption: RedemptionRecord
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
        }
    }
}