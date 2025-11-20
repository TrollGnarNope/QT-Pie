package com.veigar.questtracker.ui.component.leaderboards

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.R
import com.veigar.questtracker.model.ChildData
import com.veigar.questtracker.model.LeaderboardModel
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.model.toComposeColor
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.theme.DailyQuestGradientEnd
import com.veigar.questtracker.ui.theme.DailyQuestGradientStart
import com.veigar.questtracker.ui.theme.OneTimeQuestGradientEnd
import com.veigar.questtracker.ui.theme.OneTimeQuestGradientStart
import com.veigar.questtracker.ui.theme.WeeklyQuestGradientEnd
import com.veigar.questtracker.ui.theme.WeeklyQuestGradientStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.ExperimentalTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalTime::class)
@Composable
fun LeaderboardList(leaderboardData: List<LeaderboardModel>, selected: TimeTabOption) {
    if (leaderboardData.isEmpty()){
        return
    }
    val scrollState = rememberScrollState()

    // Remember expanded states for all leaderboards
    var expandedStates by remember { mutableStateOf(List(leaderboardData.size) { false }) }

    val coroutineScope = rememberCoroutineScope()

    // Reset expanded states when `selected` changes
    LaunchedEffect(selected) {
        expandedStates = List(leaderboardData.size) { false } // reset to all collapsed
        // Scroll to the top when the selection changes
        coroutineScope.launch { scrollState.scrollTo(0) }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        val currentDate = LocalDate.now()

        // Filter the leaderboard data based on the selected time range
        val filteredLeaderboard = leaderboardData.filter { leaderboard ->
            // Convert lastUpdated to LocalDate
            val lastUpdated = leaderboard.lastUpdated
            val lastUpdatedDate = Instant.ofEpochMilli(lastUpdated).atZone(ZoneId.systemDefault()).toLocalDate()

            // Compare based on selected time range
            when (selected) {
                TimeTabOption.DAILY -> lastUpdatedDate.isEqual(currentDate) // Check if it's the same day
                TimeTabOption.WEEKLY -> lastUpdatedDate.isAfter(currentDate.minus(7, ChronoUnit.DAYS)) // Check if it's within the past week
                TimeTabOption.MONTHLY -> lastUpdatedDate.isAfter(currentDate.minus(30, ChronoUnit.DAYS)) // Check if it's within the past 30 days
            }
        }

        // Display the filtered leaderboard
        filteredLeaderboard.forEachIndexed { index, leaderboard ->
            LeaderboardItem(
                leaderboard = leaderboard,
                position = index + 1, // Position based on filtered list
                selected = selected,
                isExpanded = expandedStates[index],
                onExpandClick = { newExpandedState ->
                    expandedStates = expandedStates.toMutableList().apply { this[index] = newExpandedState }
                }
            )
        }
    }

}

@Composable
fun LeaderboardItem(
    leaderboard: LeaderboardModel,
    position: Int,
    selected: TimeTabOption,
    isExpanded: Boolean,
    onExpandClick: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        val bgColor = when (position) {
            1 -> {
                Brush.horizontalGradient(
                    colors = listOf(DailyQuestGradientStart, DailyQuestGradientEnd)
                )
            }
            2 -> {
                Brush.horizontalGradient(
                    colors = listOf(WeeklyQuestGradientStart, WeeklyQuestGradientEnd)
                )
            }
            else -> {
                Brush.horizontalGradient(
                    colors = listOf(OneTimeQuestGradientStart, OneTimeQuestGradientEnd)
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().background(bgColor).animateContentSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                LeaderboardHeader(
                    position = position,
                    parent = leaderboard.parentModel,
                    onExpandClick = { onExpandClick(!isExpanded) }, // Toggle the expanded state
                    isExpanded = isExpanded
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Expanded child sections
                if (isExpanded) {
                    leaderboard.childList.forEach { child ->
                        ChildSection(childData = child, selected)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardHeader(position: Int, parent: UserModel, onExpandClick: () -> Unit, isExpanded: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "#$position",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                shadow = Shadow(
                    Color.Black.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier.padding(end = 12.dp)
        )

        // Avatar and Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            DisplayAvatar(
                fullAssetPath = parent.avatarUrl,
                size = 50.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = parent.name ?: "Parent Name",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(
                        Color.Black.copy(alpha = 0.3f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        // Expand button
        IconButton(onClick = onExpandClick) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = "Expand/Collapse",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ChildSection(childData: ChildData, selected: TimeTabOption) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(childData.model.firstColor.toComposeColor(), childData.model.secondColor.toComposeColor())
                    )
                )
        ){
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                DisplayAvatar(
                    fullAssetPath = childData.model.avatarUrl,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = childData.model.name,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lv. ${childData.model.level}",
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
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(color = Color(0x33000000))
                        .border(
                            width = 1.dp,
                            color = Color(0x4DFFFFFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Image(
                        painter = when (selected) {
                            TimeTabOption.DAILY -> painterResource(id = R.drawable.day)
                            TimeTabOption.WEEKLY -> painterResource(id = R.drawable.week)
                            TimeTabOption.MONTHLY -> painterResource(id = R.drawable.month)
                        },
                        contentDescription = "selected",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tasks: ${
                            when (selected) {
                                TimeTabOption.DAILY -> childData.dailyTask
                                TimeTabOption.WEEKLY -> childData.weeklyTask
                                TimeTabOption.MONTHLY -> childData.monthlyTask
                            }}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}