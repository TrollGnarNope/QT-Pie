package com.veigar.questtracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.LeaderboardsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.ChildData
import com.veigar.questtracker.model.LeaderboardModel
import com.veigar.questtracker.model.LeaderboardPrize
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.model.toHexString
import com.veigar.questtracker.ui.component.leaderboards.TimeTabOption
import com.veigar.questtracker.ui.component.pickRandomColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class LeaderBoardsViewModel(): ViewModel() {

    private val _weekly_prize = MutableStateFlow<LeaderboardPrize>(LeaderboardPrize())
    val weekly_prize: MutableStateFlow<LeaderboardPrize> = _weekly_prize

    private val _monthly_prize = MutableStateFlow<LeaderboardPrize>(LeaderboardPrize())
    val monthly_prize: MutableStateFlow<LeaderboardPrize> = _monthly_prize

    private val _leaderboards = MutableStateFlow<List<LeaderboardModel>>(emptyList())
    val leaderboards: MutableStateFlow<List<LeaderboardModel>> = _leaderboards

    private val _isLoading = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = _isLoading

    private val user = MutableStateFlow<UserModel?>(null)

    private val selected = MutableStateFlow(TimeTabOption.DAILY)
    fun setSelected(option: TimeTabOption) {
        selected.value = option
    }

    init {
        viewModelScope.launch {
            user.value = UserRepository.getUserProfile()
            _weekly_prize.value = LeaderboardsRepository.getWeeklyPrize() ?: LeaderboardPrize()
            _monthly_prize.value = LeaderboardsRepository.getMonthlyPrize() ?: LeaderboardPrize()
            if (user.value != null) {
                if(user.value!!.role == "parent"){
                    uploadLeaderboard()
                }
            }
        }
        getAllLeaderboards()
        sortLeaderboards()
//        generateRandomData() //for testing only don't uncomment if you don't know
    }

    private fun uploadLeaderboard(){
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Starting leaderboard upload")
            _isLoading.value = true

            var childList = emptyList<ChildData>()
            val lastUpdated = System.currentTimeMillis()
            val children = UserRepository.getLinkedChildren().getOrNull()
            Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Fetched linked children: $children")
            if (children != null) {
                for (child in children) {
                    Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Processing child: ${child.getDecodedUid()}")
                    val tasks = LeaderboardsRepository.getChildClaimedTasks(child.getDecodedUid())
                    Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Fetched tasks for child ${child.getDecodedUid()}: ${tasks.size}")
                    var daily = 0
                    var weekly = 0
                    var monthly = 0
                    for (task in tasks) {
                        val taskCompletionStatus = task.completedStatus
                        val completedAtTimestamp = taskCompletionStatus?.completedAt
                        if (completedAtTimestamp != null) {
                            val dateCompleted = Calendar.getInstance().apply { timeInMillis = completedAtTimestamp }
                            val today = Calendar.getInstance()
                            Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Task completed at: $dateCompleted")

                            // Check if dateCompleted is today
                            if (dateCompleted.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                dateCompleted.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                                daily++
                            }

                            // Check if dateCompleted is within the current week
                            val startOfWeek = Calendar.getInstance().apply {
                                time = today.time
                                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                            }
                            val endOfWeek = Calendar.getInstance().apply {
                                time = startOfWeek.time
                                add(Calendar.DAY_OF_WEEK, 6)
                            }
                            if (dateCompleted.after(startOfWeek) && dateCompleted.before(endOfWeek) || dateCompleted.equals(startOfWeek) || dateCompleted.equals(endOfWeek)) {
                                weekly++
                            }

                            // Check if dateCompleted is within the current month
                            if (dateCompleted.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                dateCompleted.get(Calendar.MONTH) == today.get(Calendar.MONTH)) {
                                monthly++
                            }
                        }
                    }
                    val childData = ChildData(
                        model = UserModel(
                            name = child.name,
                            level = child.level,
                            avatarUrl = child.avatarUrl,
                            firstColor = child.firstColor,
                            secondColor = child.secondColor,
                        ),
                        totalTask = tasks.size,
                        dailyTask = daily,
                        weeklyTask = weekly,
                        monthlyTask = monthly
                    )
                    childList = childList + childData
                    Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Added child data to list: $childData")
                }
            }
            val leaderboardModel = LeaderboardModel(
                leaderboardId = user.value?.getDecodedUid(),
                parentModel = user.value!!,
                childList = childList,
                lastUpdated = lastUpdated
            )
            Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Created leaderboard model: $leaderboardModel")
            val result = LeaderboardsRepository.uploadLeaderboardData(leaderboardModel)
            if (result.isSuccess) {
                Log.d("LeaderBoardsViewModel", "uploadLeaderboard: Leaderboard data uploaded successfully")
            } else {
                Log.e("LeaderBoardsViewModel", "uploadLeaderboard: Failed to upload leaderboard data", result.exceptionOrNull())
            }
            _isLoading.value = false
        }
    }

    private fun getAllLeaderboards(){
        viewModelScope.launch {
            LeaderboardsRepository.getAllLeaderboardData().collect {
                _leaderboards.value = it
            }
        }
    }

    private fun sortLeaderboards() {
        combine(
            _leaderboards,
            selected
        ) { leaderboards, selected ->
            _isLoading.value = true

            val sortedLeaderboards = leaderboards.sortedWith(compareByDescending { leaderboard ->
                // Calculate total level (contributes 20%)
                val totalChildLevel = leaderboard.childList.sumOf { it.model.level }

                // Calculate total tasks for the selected period (contributes 80%)
                val totalTasksForPeriod = when (selected) {
                    TimeTabOption.DAILY -> leaderboard.childList.sumOf { it.dailyTask }
                    TimeTabOption.WEEKLY -> leaderboard.childList.sumOf { it.weeklyTask }
                    TimeTabOption.MONTHLY -> leaderboard.childList.sumOf { it.monthlyTask }
                }

                // Calculate the weighted score
                // 20% for the Level and 80% for the total tasks
                val weightedScore = (totalChildLevel * 0.20) + (totalTasksForPeriod * 0.80)

                weightedScore
            })

            _leaderboards.value = sortedLeaderboards
            _isLoading.value = false
        }.launchIn(viewModelScope)
    }

    private fun generateRandomData() {
        viewModelScope.launch(Dispatchers.IO) {
            val random = java.util.Random()
            val dummyLeaderboards = mutableListOf<LeaderboardModel>()

            for (i in 1..5) { // Generate 5 random leaderboards
                val parent = UserModel(
                    uid = UUID.randomUUID().toString(),
                    name = "Parent ${random.nextInt(100)}",
                    role = "parent",
                    avatarUrl = "https://picsum.photos/seed/${random.nextInt(1000)}/200",
                )
                val children = mutableListOf<ChildData>()

                for (j in 1..(random.nextInt(3) + 1)) { // Generate 1 to 3 children
                    val childModel = UserModel(
                        name = "Child ${random.nextInt(100)}",
                        role = "child",
                        level = random.nextInt(5) + 1,
                        avatarUrl = "https://picsum.photos/seed/${random.nextInt(1000)}/200",
                        firstColor = pickRandomColor().toHexString(),
                        secondColor = pickRandomColor().toHexString()
                    )
                    val childData = ChildData(
                        model = childModel,
                        totalTask = random.nextInt(50),
                        dailyTask = random.nextInt(10),
                        weeklyTask = random.nextInt(20),
                        monthlyTask = random.nextInt(30)
                    )
                    children.add(childData)
                }

                dummyLeaderboards.add(
                    LeaderboardModel(
                        leaderboardId = parent.uid,
                        parentModel = parent,
                        childList = children,
                        lastUpdated = System.currentTimeMillis() - random.nextInt(1000 * 60 * 60 * 24 * 7) // last updated within the last week
                    )
                )
            }

            // Now, we upload the leaderboards one by one (ensuring completion)
            for (leaderboard in dummyLeaderboards) {
                val result = LeaderboardsRepository.uploadLeaderboardData(leaderboard)
                if (result.isFailure) {
                    // Handle error if upload failed
                    Log.e("LeaderboardUpload", "Failed to upload leaderboard: ${leaderboard.leaderboardId}")
                } else {
                    Log.d("LeaderboardUpload", "Successfully uploaded leaderboard: ${leaderboard.leaderboardId}")
                }
            }
        }
    }
}