package com.veigar.questtracker.ui.screen.leaderboards

import android.os.Build
import android.os.CountDownTimer
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.model.LeaderboardPrize
import com.veigar.questtracker.ui.component.leaderboards.LeaderboardList
import com.veigar.questtracker.ui.component.leaderboards.PrizeBanner
import com.veigar.questtracker.ui.component.leaderboards.TimeTab
import com.veigar.questtracker.ui.component.leaderboards.TimeTabOption
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.viewmodel.LeaderBoardsViewModel
import java.time.LocalDateTime
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardsScreen(navController: NavController, viewModel: LeaderBoardsViewModel = viewModel()) {
    var prize by remember { mutableStateOf(LeaderboardPrize())}
    var selectedTab by remember { mutableStateOf(TimeTabOption.DAILY) }

    var countdownText by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState(initial = true)
    val leaderboardsData by viewModel.leaderboards.collectAsState()

    LaunchedEffect(selectedTab) {
        val countdownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTick(millisUntilFinished: Long) {
                countdownText = when (selectedTab) {
                    TimeTabOption.DAILY ->
                        getTimeUntilNextDay()
                    TimeTabOption.WEEKLY ->
                        getTimeUntilNextWeek()
                    TimeTabOption.MONTHLY ->
                        getTimeUntilEndOfMonth()
                }
            }

            override fun onFinish() {
                // Handle the countdown finishing (optional)
            }
        }
        countdownTimer.start()
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            TimeTabOption.DAILY -> {
                prize = LeaderboardPrize(
                    leaderboardPrizeId = "dummy",
                    title = "DAILY",
                    prizeText = "....",
                    iconURL = "error"
                )
            }
            TimeTabOption.WEEKLY -> {
                prize = viewModel.weekly_prize.value
            }
            TimeTabOption.MONTHLY -> {
                prize = viewModel.monthly_prize.value
            }
        }
    }

    BackHandler {
        navController.popBackStack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CoralBlueDark
                ),
                title = {
                    Text(
                        text = "Leaderboards",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .background(CoralBlueDark)
                .fillMaxSize()
        ){
            TimeTab(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                onTabSelected = {
                    selectedTab = it
                    viewModel.setSelected(it)
                }
            )
            PrizeBanner(
                title = prize.title,
                prizeText = prize.prizeText,
                iconUrl = prize.iconURL,
                endsInText = "Ends in: $countdownText"
            )
            LeaderboardList(leaderboardsData, selectedTab)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getTimeUntilNextDay(): String {
    val now = LocalDateTime.now()
    val nextDay = now.toLocalDate().plusDays(1).atStartOfDay() // Start of next day (midnight)
    val remaining = java.time.Duration.between(now, nextDay)
    return formatDuration(remaining)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getTimeUntilNextWeek(): String {
    val now = LocalDateTime.now()
    val daysUntilSunday = (7 - now.dayOfWeek.ordinal) % 7
    val nextSunday = if (daysUntilSunday == 0) {
        now.plusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0)
    } else {
        now.plusDays(daysUntilSunday.toLong()).withHour(0).withMinute(0).withSecond(0).withNano(0)
    }
    val remaining = java.time.Duration.between(now, nextSunday)
    return formatDuration(remaining)
}

// Function to get remaining time until the end of the current month (last day of the month)
@RequiresApi(Build.VERSION_CODES.O)
fun getTimeUntilEndOfMonth(): String {
    val now = LocalDateTime.now()
    val endOfMonth = LocalDateTime.of(now.year, now.month, now.toLocalDate().lengthOfMonth(), 23, 59, 59, 0)
    val remaining = java.time.Duration.between(now, endOfMonth)
    return formatDuration(remaining)
}

// Function to format the time remaining (days, hours, minutes)
@RequiresApi(Build.VERSION_CODES.O)
fun formatDuration(duration: java.time.Duration): String {
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    return "%d D, %d h, %d m".format(days, hours, minutes)
}

