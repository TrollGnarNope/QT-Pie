package com.veigar.questtracker.ui.component.child

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veigar.questtracker.R
import kotlinx.coroutines.delay

@Composable
fun PreviousDaySummaryDialog(
    showDialog: Boolean,
    missedTasksCount: Int,
    completedTasksCount: Int,
    declinedTasksCount: Int,
    totalHpReduced: Int,
    totalHpHealed: Int,
    onDismissRequest: () -> Unit,
    autoDismissDelay: Long? = 5000L
) {
    var dismissOnClickOutsideEnabled by remember { mutableStateOf(autoDismissDelay == null) }

    LaunchedEffect(autoDismissDelay) {
        autoDismissDelay?.let {
            if (it > 0) {
                delay(it)
                dismissOnClickOutsideEnabled = true
            }
        }
    }

    if (showDialog && (missedTasksCount > 0 || completedTasksCount > 0 || declinedTasksCount > 0 || totalHpReduced > 0 || totalHpHealed > 0)) {
        Dialog(
            onDismissRequest = {
                if (dismissOnClickOutsideEnabled) {
                    onDismissRequest()
                }
            }, properties = DialogProperties(
                dismissOnClickOutside = dismissOnClickOutsideEnabled, dismissOnBackPress = dismissOnClickOutsideEnabled,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.to_do_list),
                        contentDescription = "Summary",
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Recap",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (completedTasksCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.check),
                                contentDescription = "Completed Tasks Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Tasks Completed: $completedTasksCount",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Missed Tasks
                    if (missedTasksCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.decline),
                                contentDescription = "Missed Tasks Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Tasks Missed: $missedTasksCount",
                                color = Color(0xFFF44336),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (declinedTasksCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(painter = painterResource(id = R.drawable.decline),
                                contentDescription = "Declined Tasks Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Tasks Declined: $declinedTasksCount",
                                color = Color(0xFFFF9800),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp)) // Consistent spacing
                    }

                    // Total Points Reduced
                    if (totalHpReduced > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.decline),
                                contentDescription = "Points Reduced Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Total Points Lost: $totalHpReduced",
                                color = Color(0xFFF44336), // Red for loss
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Total Points Gained
                    if (totalHpHealed > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.approved),
                                contentDescription = "Points Gained Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Total Points Gained: $totalHpHealed",
                                color = Color(0xFF4CAF50), // Green for gain
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (completedTasksCount > 0 || missedTasksCount > 0 || declinedTasksCount > 0 || totalHpReduced > 0 || totalHpHealed > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (dismissOnClickOutsideEnabled) {
                        Text(
                            "Tap outside to Continue",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(4.dp)
                        )
                    }
                }
            }
        }
    }
}
