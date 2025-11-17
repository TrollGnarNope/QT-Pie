package com.veigar.questtracker.ui.component.tasks

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.R
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.ui.component.AssetCategoryImage
import com.veigar.questtracker.util.getNextResetInfo

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun QuestCard(
    task: TaskModel,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
    showResetChip: Boolean = true
) {
    var completedStatus by remember { mutableStateOf(task.completedStatus) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFDFF6FF),
                            Color(0xFFB3E5FC)
                        )
                    )
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier.padding(start = 10.dp, end = 10.dp).size(36.dp)
            when (if (showStatus) completedStatus?.status else TaskStatus.PENDING) {
                TaskStatus.PENDING, null ->
                    AssetCategoryImage(
                        imageNameWithExtension = "${task.icon}.png",
                        contentDescription = "icon",
                        size = 36.dp,
                        modifier = iconModifier
                    )
                else -> {
                    val painterId = when (completedStatus?.status) {
                        TaskStatus.AWAITING_APPROVAL -> R.drawable.magnifying_glass
                        TaskStatus.COMPLETED -> R.drawable.check
                        TaskStatus.WAITING_FOR_RESET -> R.drawable.check
                        TaskStatus.DECLINED -> R.drawable.decline
                        else -> R.drawable.decline
                    }
                    Image(
                        painter = painterResource(id = painterId),
                        contentDescription = "icon",
                        modifier = iconModifier
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF37474F),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFBBDEFB)
                    ) {
                        Text(
                            text = "✦ +${task.rewards.xp} XP",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            color = Color(0xFF0D47A1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFFF9C4)
                    ) {
                        Text(
                            text = "⭐ +${task.rewards.coins}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            color = Color(0xFFF57F17),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (showStatus && completedStatus?.status != null && completedStatus?.status != TaskStatus.PENDING) {
                        val (surfaceColor, textColor, statusText) = when (completedStatus?.status) {
                            TaskStatus.AWAITING_APPROVAL -> Triple(Color(0xFFFFECB3), Color(0xFFFFA000), "Approval")
                            TaskStatus.COMPLETED -> Triple(Color(0xFFC8E6C9), Color(0xFF388E3C), "Completed")
                            TaskStatus.DECLINED -> Triple(Color(0xFFFFCDD2), Color(0xFFD32F2F), "Declined")
                            TaskStatus.WAITING_FOR_RESET -> if (showResetChip) Triple(Color(0xFFC8E6C9), Color(0xFF388E3C), "On Reset: ${getNextResetInfo(task)}") else Triple(Color(0xFFC8E6C9), Color(0xFF388E3C), "Completed")
                            TaskStatus.MISSED -> Triple(Color(0xFFFFCDD2), Color(0xFFD32F2F), "Missed")
                            else -> Triple(Color.Transparent, Color.Transparent, "") // Should not happen
                        }
                        if (statusText.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = surfaceColor
                            ) {
                                Text(
                                    text = statusText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    text = task.description,
                    color = Color(0xFF607D8B),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}