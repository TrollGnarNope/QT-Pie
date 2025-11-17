package com.veigar.questtracker.ui.component.tasks

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.model.TaskStatus
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.AssetCategoryImage
import com.veigar.questtracker.ui.component.ImageViewerDialog
import com.veigar.questtracker.util.ImageManager
import com.veigar.questtracker.util.getNextResetInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper to get display properties for TaskStatus
data class TaskStatusDisplay(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
)

// Simple date formatter
fun Long.toFormattedDateTime(): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mma", Locale.getDefault())
        sdf.format(Date(this))
    } catch (_: Exception) {
        "N/A"
    }
}

fun String.toFormattedDate(): String { // For ISO strings like "2025-07-04"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(this)
        if (date != null) outputFormat.format(date) else "N/A"
    } catch (_: Exception) {
        this // return original if parsing fails
    }
}

fun String.toFormattedTime(): String {
    //time is 16:02:16.976 parse in 12 hour format
    return try {
        val inputFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = inputFormat.parse(this)
        if (time != null) outputFormat.format(time) else "N/A"
    } catch (_: Exception) {
        this // return original if parsing fails
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailSheet(
    showSheet: Boolean, // Controls visibility
    onDismissRequest: () -> Unit, // Called when the sheet should be dismissed
    task: TaskModel?, // Make task nullable, as sheet might be initially hidden
    currentUserRole: String,
    childList: List<UserModel>,
    // Parent Actions
    onEditTask: ((TaskModel) -> Unit)? = null,
    onDeleteTask: ((TaskModel) -> Unit)? = null,
    onApproveTask: ((TaskModel) -> Unit)? = null,
    onDeclineTask: ((TaskModel) -> Unit)? = null,
    // Child Actions
    onSubmitForApproval: ((TaskModel) -> Unit)? = null,
    onCancelSubmission: ((TaskModel) -> Unit)? = null,
    onClaimRewards: ((TaskModel) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Optional: Makes it only fully expanded or hidden
    )

    if (showSheet && task != null) { // Only compose the ModalBottomSheet when showSheet is true and task is not null
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color(0xFFE3F2FD),
        ) {
            QuestDetailSheetContent(
                task = task,
                currentUserRole = currentUserRole,
                childList = childList,
                onDismissRequest = onDismissRequest,
                onEditTask = onEditTask,
                onDeleteTask = onDeleteTask,
                onApproveTask = onApproveTask,
                onDeclineTask = onDeclineTask,
                onSubmitForApproval = onSubmitForApproval,
                onCancelSubmission = onCancelSubmission,
                onClaimRewards = onClaimRewards
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun QuestDetailSheetContent(
    task: TaskModel,
    currentUserRole: String,
    childList : List<UserModel>,
    onDismissRequest: () -> Unit, // To dismiss the sheet after an action
    // Parent Actions
    onEditTask: ((TaskModel) -> Unit)?,
    onDeleteTask: ((TaskModel) -> Unit)?,
    onApproveTask: ((TaskModel) -> Unit)?,
    onDeclineTask: ((TaskModel) -> Unit)?,
    // Child Actions
    onSubmitForApproval: ((TaskModel) -> Unit)?,
    onCancelSubmission: ((TaskModel) -> Unit)?,
    onClaimRewards: ((TaskModel) -> Unit)?
) {

    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val screenHeight = configuration.screenHeightDp.dp

    val completedStatus by remember { mutableStateOf(task.completedStatus) }
    var showImageViewer by remember { mutableStateOf(false) }
    var downloadedImagePath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ) // Better horizontal padding, reduced vertical
            .fillMaxWidth()
            .heightIn(max = screenHeight * 0.9f) // Allow sheet to take up to 90% of screen height
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetCategoryImage(
                imageNameWithExtension = "${task.icon}.png",
                contentDescription = "icon",
                size = 36.dp,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
            )

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
                    if (completedStatus?.status != null && completedStatus?.status != TaskStatus.PENDING) {
                        val (surfaceColor, textColor, statusText) = when (completedStatus?.status) {
                            TaskStatus.AWAITING_APPROVAL -> Triple(
                                Color(0xFFFFECB3),
                                Color(0xFFFFA000),
                                "Approval"
                            )

                            TaskStatus.COMPLETED -> Triple(
                                Color(0xFFC8E6C9),
                                Color(0xFF388E3C),
                                "Completed"
                            )

                            TaskStatus.DECLINED -> Triple(
                                Color(0xFFFFCDD2),
                                Color(0xFFD32F2F),
                                "Declined"
                            )

                            TaskStatus.WAITING_FOR_RESET -> Triple(
                                Color(0xFFC8E6C9),
                                Color(0xFF388E3C),
                                "On Reset: ${getNextResetInfo(task)}"
                            )

                            TaskStatus.MISSED -> Triple(
                                Color(0xFFFFCDD2),
                                Color(0xFFD32F2F),
                                "Missed"
                            )

                            else -> Triple(
                                Color.Transparent,
                                Color.Transparent,
                                ""
                            ) // Should not happen
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        Text(
            text = "Schedule and Details",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
        )
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            task.startDate?.takeIf { it.isNotBlank() }?.let {
                InfoRow(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Starts:",
                    value = it.toFormattedDate()
                )
            }
            task.endDate?.takeIf { it.isNotBlank() }?.let {
                InfoRow(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Ends:",
                    value = it.toFormattedDate()
                )
            }
            task.reminderTime?.takeIf { it.isNotBlank() }?.let {
                InfoRow(
                    icon = Icons.Filled.Notifications,
                    label = "Reminder:",
                    value = it.toFormattedTime()
                )
            }
            task.repeat?.let {
                InfoRow(
                    icon = Icons.Filled.Repeat,
                    label = "Repeats:",
                    value = it.toString(),
                    isMultiline = true
                )
            }
            Log.d("TaskModel", "AssignedTo: ${task.assignedTo}")
            if (task.assignedTo.isNotBlank()) {
                val assignedSet = task.assignedTo.substring(
                    1,
                    task.assignedTo.length - 1
                ) // Remove brackets: "uid1, uid2"
                    .split(',')                                 // Split by comma
                    .map { it.trim() }                          // Trim whitespace
                    .filter { it.isNotEmpty() }                 // Filter out empty strings
                    .toSet()

                var assignedChildString = ""
                if (currentUserRole == "parent") {
                    val assignedChild = childList.filter {
                        assignedSet.contains(it.getDecodedUid())
                    }
                    assignedChildString = assignedChild.joinToString { it.name }
                } else {
                    assignedChildString = "You" + if (assignedSet.size > 1) " and others" else ""
                }
                Log.d("TaskModel", "AssignedTo: $assignedChildString")
                InfoRow(
                    icon = Icons.Filled.ChildCare,
                    label = "For:",
                    value = assignedChildString,
                    isMultiline = true
                )
            }
            InfoRow(
                icon = Icons.Filled.CalendarToday,
                label = "Created:",
                value = task.createdAt.toFormattedDateTime()
            )
            if (!completedStatus?.proofLink.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        completedStatus?.proofLink?.let { relativePath ->
                            // Use the full URL directly instead of downloading
                            val fullUrl = ImageManager.getFullUrl(relativePath)
                            downloadedImagePath = fullUrl
                            showImageViewer = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.OpenInNew,
                        contentDescription = "View Proof",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("View Proof of Completion")
                }
            }

            // If nanny approval was required
            if (completedStatus?.nannyApprove == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This submission required Nanny approval.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Action Buttons
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentUserRole) {
                "parent" -> {
                    if (completedStatus?.status == TaskStatus.AWAITING_APPROVAL) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            onDeclineTask?.let {
                                ActionButton(
                                    text = "Decline", // Shorter text
                                    onClick = { it(task); onDismissRequest() },
                                    buttonColor = Color(0xFFFFCDD2), // Soft Red
                                    contentColor = Color(0xFFD32F2F), // Darker Red for text
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            onApproveTask?.let {
                                ActionButton(
                                    text = "Approve", // Shorter text
                                    onClick = { it(task); onDismissRequest() },
                                    buttonColor = Color(0xFFC8E6C9), // Soft Green
                                    contentColor = Color(0xFF388E3C), // Darker Green for text
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Edit and Delete can also be in a row or separate if one is less common
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (completedStatus?.status == TaskStatus.PENDING || completedStatus?.status == TaskStatus.DECLINED) {
                            onEditTask?.let {
                                ActionButton(
                                    text = "Edit",
                                    onClick = { it(task); onDismissRequest() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Add a Spacer to keep delete button alignment if edit is not present
                            // Or adjust logic if delete should only show with edit
                            if (onDeleteTask != null) Spacer(Modifier.weight(1f))
                        }
                        if (completedStatus?.status != TaskStatus.AWAITING_APPROVAL) {
                            onDeleteTask?.let {
                                // Make delete less prominent or ensure it's clearly destructive
                                OutlinedButton( // Example: Using OutlinedButton for Delete
                                    onClick = { it(task); onDismissRequest() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }

                "child" -> {
                    if (completedStatus?.status == TaskStatus.PENDING || completedStatus?.status == TaskStatus.DECLINED || completedStatus?.status == TaskStatus.MISSED) {
                        onSubmitForApproval?.let {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (completedStatus?.status == TaskStatus.MISSED) {
                                    Text(
                                        text = "You previously missed this task. Please comply.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                ActionButton(
                                    text = "Submit for Approval",
                                    onClick = { it(task); onDismissRequest() },
                                    buttonColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth() // Full width if it's the only primary action
                                )
                            }
                        }
                    }
                    if (completedStatus?.status == TaskStatus.AWAITING_APPROVAL) {
                        onCancelSubmission?.let {
                            ActionButton(
                                text = "Cancel Submission",
                                onClick = { it(task); onDismissRequest() },
                                buttonColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth() // Full width if it's the only primary action
                            )
                        }
                    }
                    if (completedStatus?.status == TaskStatus.COMPLETED) {
                        onClaimRewards?.let {
                            ActionButton(
                                text = "CLAIM REWARDS",
                                onClick = { it(task); onDismissRequest() },
                                buttonColor = Color(0xFFC8E6C9), // Soft Green
                                contentColor = Color(0xFF388E3C), // Darker Green for text
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) // Add space for system navigation

        // Image Viewer Dialog
        if (showImageViewer && downloadedImagePath != null) {
            ImageViewerDialog(
                imagePath = downloadedImagePath!!,
                title = "Proof of Completion - ${task.title}",
                onDismiss = {
                    showImageViewer = false
                    downloadedImagePath = null
                }
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String?,
    isMultiline: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueFontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize
) {
    if (value.isNullOrBlank()) return

    Row(verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .size(18.dp)
                .padding(top = if (isMultiline) 2.dp else 0.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = valueFontSize),
            color = valueColor,
            modifier = Modifier.weight(0.6f),
            maxLines = if (isMultiline) 5 else 1,
            overflow = if (isMultiline) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary // Default content color
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = contentColor
        )
    ) {
        Text(text)
    }
}
