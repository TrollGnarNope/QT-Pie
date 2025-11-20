
package com.veigar.questtracker.ui.screen.parent.tab

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayLight
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.ProfessionalGrayTextSecondary
import com.veigar.questtracker.ui.theme.ProfessionalGrayBorder
import com.veigar.questtracker.ui.theme.ProfessionalGraySurface
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.model.toComposeColor
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel.ChildTaskProgress
import java.util.Calendar
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChildrenTab(
    navController: NavController,
    viewModel: ParentDashboardViewModel
) {
    val userModel by viewModel.user.collectAsStateWithLifecycle()
    val childUsers by viewModel.linkedChildren.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingChildren.collectAsStateWithLifecycle()
    val errorFetching by viewModel.errorFetchingChildren.collectAsStateWithLifecycle()

    val showUnlinkDialog = remember { mutableStateOf(false) }
    val childToUnlink = remember { mutableStateOf<UserModel?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isLoading,
        onRefresh = { viewModel.fetchLinkedChildren() }
    ){
        Column(
            modifier = Modifier
                .background(ProfessionalGrayDark) // Professional gray background
                .fillMaxSize()
                .padding(top = 16.dp) // Add padding to the top of the entire screen content
                .verticalScroll(rememberScrollState())
        ) {
            // Greeting Container
            ChildFriendlyGreetingCard(
                userModel = userModel!!,
                modifier = Modifier
            )

            // Overall Task Progress Summary
            if (childUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                val totalProgress by viewModel.overallTaskProgress.collectAsStateWithLifecycle()
                if (totalProgress.total > 0) {
                    OverallProgressCard(
                        totalCompleted = totalProgress.completed,
                        totalAwaitingApproval = totalProgress.awaitingApproval,
                        totalDeclined = totalProgress.declined,
                        totalMissed = totalProgress.missed,
                        totalOngoing = totalProgress.ongoing,
                        totalTasks = totalProgress.total,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.animateContentSize().padding(horizontal = 0.dp, vertical = 16.dp)) {
                when {
                    isLoading -> {
                        ChildFriendlyLoadingIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp, bottom = 32.dp, start = 16.dp, end = 16.dp) // Give it some space
                        )
                    }

                    // 2. ERROR STATE: Show an error message if fetching failed
                    //    Only show if not loading and error is present.
                    !isLoading && errorFetching != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Oh no! We couldn't find the adventurers.\nError: $errorFetching",
                                style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // 3. EMPTY STATE: No children found after loading and no error
                    !isLoading && childUsers.isEmpty() && errorFetching == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No little adventurers found yet!",
                                    style = MaterialTheme.typography.titleMedium, // Slightly larger for emphasis
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the '+' card to add a child and start their quest!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                AddChildCard(
                                    onClick = { navController.navigate(NavRoutes.LinkChild.route) }
                                )
                            }
                        }
                        // The AddChildCard will be shown by the LazyRow logic if childUsers is empty.
                        // If you want it always visible even here, you could add it:
                        // AddChildCard(onClick = { navController.navigate(NavRoutes.LinkChild.route) })
                    }

                    // 4. CONTENT STATE: Children are loaded and available
                    else -> {
                        LazyRow(
                            state = rememberLazyListState(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(childUsers.size) { index ->
                                val child = childUsers[index]

                                val location = viewModel.getChildNearestLocation(child.getDecodedUid())
                                val taskProgress = viewModel.getChildTaskProgress(child.getDecodedUid())

                                ChildCard(
                                    child = child,
                                    location = location,
                                    taskProgress = taskProgress,
                                    onUnlink = {
                                        childToUnlink.value = child
                                        showUnlinkDialog.value = true
                                    },
                                    onViewCompletedTasks = {
                                        navController.navigate(NavRoutes.CompletedTasks.createRoute(child.getDecodedUid()))
                                    },
                                    onViewLocationHistory = {
                                        val parentId = viewModel.user.value?.getDecodedUid() ?: ""
                                        navController.navigate(NavRoutes.LocationHistory.createRoute(parentId, child.getDecodedUid()))
                                    },
                                    onLocationClick = {
                                        viewModel.setFocusedChild(child.getDecodedUid())
                                        viewModel.setCurrentTab("geofence")
                                    }
                                )
                            }
                            // Still include AddChildCard if it's separate from the childUsers list
                            item {
                                AddChildCard(
                                    onClick = { navController.navigate(NavRoutes.LinkChild.route) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Manage Rewards and Quizzes Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ManageRewardsCard(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navController.navigate(NavRoutes.Rewards.route)
                    }
                )
                ManageQuizzesCard(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navController.navigate(NavRoutes.Quizzes.route)
                    }
                )
            }

            if (showUnlinkDialog.value && childToUnlink.value != null) {
                AlertDialog(
                    onDismissRequest = {
                        showUnlinkDialog.value = false
                        childToUnlink.value = null
                    },
                    title = { Text("Confirm Unlink") },
                    text = { Text("Are you sure you want to unlink ${childToUnlink.value?.name}?") },
                    confirmButton = {
                        TextButton(onClick = {
                            childToUnlink.value?.let { child ->
                                viewModel.unlinkChild(child.getDecodedUid())
                            }
                            showUnlinkDialog.value = false
                            childToUnlink.value = null
                        }) {
                            Text("Unlink")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnlinkDialog.value = false; childToUnlink.value = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
@Composable
fun OverallProgressCard(
    totalCompleted: Int,
    totalAwaitingApproval: Int,
    totalDeclined: Int,
    totalMissed: Int,
    totalOngoing: Int,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    val completionPercentage = if (totalTasks > 0) {
        (totalCompleted.toFloat() / totalTasks.toFloat()) * 100f
    } else {
        0f
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = Color(0x33000000))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All Children's Tasks",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${totalCompleted}/${totalTasks}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${completionPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color = Color(0x4DFFFFFF))
                .fillMaxWidth()
        ) {
            val progressRatio = if (totalTasks > 0) {
                totalCompleted.toFloat() / totalTasks.toFloat()
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detailed Breakdown
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Completed
            if (totalCompleted > 0) {
                ProgressBreakdownRow(
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF4CAF50),
                    label = "Completed",
                    count = totalCompleted,
                    total = totalTasks
                )
            }
            
            // Awaiting Approval
            if (totalAwaitingApproval > 0) {
                ProgressBreakdownRow(
                    icon = Icons.Default.PendingActions,
                    iconColor = Color(0xFFFFA726),
                    label = "Awaiting Approval",
                    count = totalAwaitingApproval,
                    total = totalTasks
                )
            }
            
            // Ongoing
            if (totalOngoing > 0) {
                ProgressBreakdownRow(
                    icon = Icons.Default.PlayCircle,
                    iconColor = Color(0xFF42A5F5),
                    label = "Ongoing",
                    count = totalOngoing,
                    total = totalTasks
                )
            }
            
            // Declined
            if (totalDeclined > 0) {
                ProgressBreakdownRow(
                    icon = Icons.Default.PauseCircle,
                    iconColor = Color(0xFFEF5350),
                    label = "Declined",
                    count = totalDeclined,
                    total = totalTasks
                )
            }
            
            // Missed
            if (totalMissed > 0) {
                ProgressBreakdownRow(
                    icon = Icons.Default.HourglassEmpty,
                    iconColor = Color(0xFFE57373),
                    label = "Missed",
                    count = totalMissed,
                    total = totalTasks
                )
            }
        }
    }
}

@Composable
private fun ProgressBreakdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    count: Int,
    total: Int
) {
    val percentage = if (total > 0) {
        (count.toFloat() / total.toFloat()) * 100f
    } else {
        0f
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.9f)
                )
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun ManageRewardsCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF4A90A4), Color(0xFF1E5A6B)) // Toned down blue gradient
                )
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically // Align icon and text vertically
    ) {
        Icon(
            imageVector = Icons.Filled.Cake, // Cake icon for rewards
            contentDescription = "Manage Rewards",
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .padding(start = 12.dp) // Add padding to the left of the icon
        )
        Spacer(modifier = Modifier.width(8.dp)) // Space between icon and text
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp) // Padding for the text column
        ) {
            Text(
                text = "Manage Rewards",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold, 
                    color = Color.White,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Manage and view",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ManageQuizzesCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A)) // Purple gradient for quizzes
                )
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically // Align icon and text vertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle, // CheckCircle icon for quizzes
            contentDescription = "Manage Quizzes",
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .padding(start = 12.dp) // Add padding to the left of the icon
        )
        Spacer(modifier = Modifier.width(8.dp)) // Space between icon and text
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp) // Padding for the text column
        ) {
            Text(
                text = "Manage Quizzes",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold, 
                    color = Color.White,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Create and manage",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
fun ChildCard(
    child: UserModel,
    location: String?,
    taskProgress: ChildTaskProgress = ChildTaskProgress(),
    modifier: Modifier = Modifier,
    onUnlink: (() -> Unit)? = null,
    onViewCompletedTasks: (() -> Unit)? = null,
    onViewLocationHistory: (() -> Unit)? = null,
    onLocationClick: (() -> Unit)? = null
) {
    val index = Random.nextInt(0,3)
    val gradientColors = if (child.firstColor.isEmpty() || child.secondColor.isEmpty()) {
        when (index % 3) {
            0 -> listOf(Color(0xFFFF8A80), Color(0xFFD32F2F)) // Red-ish
            1 -> listOf(Color(0xFFA5D6A7), Color(0xFF388E3C)) // Green-ish
            else -> listOf(Color(0xFF5A8A9A), Color(0xFF2C5A6A)) // Toned down blue-ish
        }
    } else {
        listOf(child.firstColor.toComposeColor(), child.secondColor.toComposeColor())
    }
    Column(
        modifier = modifier
            .width(250.dp)
            .wrapContentHeight()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .padding(16.dp)
    ) {
        // Location Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!location.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color = Color(0x33000000)) // Semi-transparent dark
                        .clickable(onClick = { onLocationClick?.invoke() })
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = location, style = TextStyle(fontSize = 12.sp, color = Color.White))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Fills space if no location
            }

            // Unlink Button
            onUnlink?.let { unlinkAction ->
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = "Unlink Child",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { unlinkAction() }
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp)) // Space after location/unlink row

        // Name and Level
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            DisplayAvatar(
                fullAssetPath = child.avatarUrl,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(8.dp)) // Space between avatar and name
            Text(
                text = child.name,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
            )
            Spacer(modifier = Modifier.weight(1f)) // Pushes the avatar and level to the right
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp)) // Space between avatar and level
                Text(
                    text = "Lv. ${child.level}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)) // Clip before background and border
                        .background(color = Color(0x33000000)) // Semi-transparent dark
                        .border(
                            width = 1.dp,
                            color = Color(0x4DFFFFFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // Space after name/level

        // XP Bar and Text
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color = Color(0x4DFFFFFF)) // Background color of the bar
                    .fillMaxWidth()
            ) {
                val xpProgress = if (child.xp > 0) child.xp.toFloat() / 100f else 0f
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(xpProgress) // Dynamic width based on XP progress
                        .clip(RoundedCornerShape(4.dp))
                        .background(color = Color(0xFFFFDA63)) // Yellow progress color
                )
            }
            Spacer(modifier = Modifier.height(4.dp)) // Small space between bar and text
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("XP", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)))
                Text("${child.xp} / 100", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task Progress Section
        if (taskProgress.total > 0) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = Color(0x33000000))
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks Progress",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${taskProgress.completed}/${taskProgress.total}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                // Progress Bar
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color = Color(0x4DFFFFFF))
                        .fillMaxWidth()
                ) {
                    val progressRatio = if (taskProgress.total > 0) {
                        taskProgress.completionPercentage.toFloat() / 100f
                    } else {
                        0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressRatio)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color = Color(0xFF4CAF50)) // Green for completed
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Detailed Breakdown - First Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Completed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${taskProgress.completed}",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        )
                    }
                    
                    // Awaiting Approval
                    if (taskProgress.awaitingApproval > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PendingActions,
                                contentDescription = "Awaiting Approval",
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${taskProgress.awaitingApproval}",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            )
                        }
                    }
                    
                    // Ongoing
                    if (taskProgress.ongoing > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Ongoing",
                                tint = Color(0xFF42A5F5),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${taskProgress.ongoing}",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            )
                        }
                    }
                }
                
                // Second Row - Declined and Missed
                if (taskProgress.declined > 0 || taskProgress.missed > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Declined
                        if (taskProgress.declined > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PauseCircle,
                                    contentDescription = "Declined",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${taskProgress.declined} declined",
                                    style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }
                        
                        // Missed
                        if (taskProgress.missed > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Missed",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${taskProgress.missed} missed",
                                    style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color = Color(0x1A000000))
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Only show Points and XP - hide HP, Attack, Magic, Defense, Stamina
            Stat("⭐ POINTS:", child.pts)

            onViewCompletedTasks?.let { action ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .background(Color(0x33000000))
                        .clickable { action() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed Tasks",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Task History",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            onViewLocationHistory?.let { action ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .background(Color(0x33000000))
                        .clickable { action() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location History",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Location History",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun Stat(label: String, value: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, style = TextStyle(fontSize = 14.sp, color = Color.White))
        Spacer(modifier = Modifier.width(10.dp)) // Increased space between label and value
        Text(text = value.toString(), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
    }
}

@Composable
fun AddChildCard(onClick : () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(200.dp) // Match width of ChildCard
            .height(200.dp) // Adjust height to roughly match ChildCard height
            .clip(RoundedCornerShape(12.dp)) // Match border radius of ChildCard
            .border(width = 2.dp, color = Color(0x66FFFFFF), shape = RoundedCornerShape(12.dp))
            .background(color = Color(0x1AFFFFFF)) // Semi-transparent background
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Child",
            tint = Color(0xFFFFFFFF).copy(alpha = 0.7f), // White with transparency
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp)) // Space between icon and text
        Text(
            text = "Add Child",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFFFFF).copy(alpha = 0.7f) // White with transparency
            )
        )
    }
}


@Composable
fun ChildFriendlyGreetingCard(userModel: UserModel, modifier: Modifier = Modifier) {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

    data class GreetingInfo(
        val text: String,
        val icon: ImageVector,
        val iconColor: Color,
        val backgroundGradient: List<Color>
    )

    val greetingInfo = when (currentHour) {
        in 0..5 -> GreetingInfo( // Late Night
            text = "Good Night",
            icon = Icons.Filled.NightsStay,
            iconColor = Color(0xFFD1C4E9),
            backgroundGradient = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)) // Very dark blue night
        )
        in 6..11 -> GreetingInfo( // Morning
            text = "Good Morning",
            icon = Icons.Filled.WbSunny,
            iconColor = Color(0xFFFFF176),
            backgroundGradient = listOf(Color(0xFF2C4A5A), Color(0xFF1A3A4A)) // Toned down morning blue
        )
        in 12..17 -> GreetingInfo( // Afternoon
            text = "Good Afternoon",
            icon = Icons.Filled.WbSunny,
            iconColor = Color(0xFFFFF176),
            backgroundGradient = listOf(Color(0xFFFF9949), Color(0xFFFF7B3C)) // Burnt orange to deep amber
        )
        else -> GreetingInfo( // Evening
            text = "Good Evening",
            icon = Icons.Filled.NightsStay,
            iconColor = Color(0xFFB3E5FC),
            backgroundGradient = listOf(Color(0xFF263238), Color(0xFF37474F)) // Charcoal to dark steel
        )
    }

    val textColorPrimary = Color.White
    val textColorSecondary = Color.White.copy(alpha = 0.85f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Maintain horizontal padding for alignment with other elements
            .clip(RoundedCornerShape(10.dp)) // Slightly less rounded if 20dp felt too much
            .background(
                brush = Brush.horizontalGradient(greetingInfo.backgroundGradient)
            )
            .padding(vertical = 8.dp, horizontal = 20.dp) // Adjusted padding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp) // Slightly less space
        ) {
            Icon(
                imageVector = greetingInfo.icon,
                contentDescription = greetingInfo.text,
                tint = greetingInfo.iconColor, // Use the specific icon color
                modifier = Modifier.size(30.dp) // Slightly smaller icon
            )
            Box(modifier = Modifier.weight(1f)) { // Use Box with weight to allow marquee to fill available space
                Text(
                    text = "${greetingInfo.text}, ${userModel.name}!",
                    style = MaterialTheme.typography.titleLarge.copy( // titleLarge is a bit smaller than headlineSmall
                        fontWeight = FontWeight.Bold, // titleLarge is already quite prominent
                        color = textColorPrimary
                    ),
                    maxLines = 1, // Ensure it doesn't wrap awkwardly if name is very long
                    modifier = Modifier.basicMarquee() // Apply marquee effect
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp)) // Slightly less space

        Text(
            text = "Let's see how your amazing children are doing today! ✨",
            style = MaterialTheme.typography.bodyMedium.copy( // bodyMedium is smaller than bodyLarge
                color = Color.White,
                lineHeight = 20.sp // Adjust line height for smaller font
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChildFriendlyLoadingIndicator(modifier: Modifier = Modifier, title: String = "Loading adventurers...", text: String = "They must be on an epic quest!") {
    val infiniteTransition = rememberInfiniteTransition(label = "hourglass_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "hourglass_rotation"
    )
    val color by infiniteTransition.animateColor(
        initialValue = Color(0xFF5A8A9A), // Toned down Light Blue
        targetValue = yellow,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "hourglass_color"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.HourglassEmpty,
            contentDescription = "Loading adventurers...",
            tint = color,
            modifier = Modifier
                .size(60.dp)
                .rotate(rotation)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
