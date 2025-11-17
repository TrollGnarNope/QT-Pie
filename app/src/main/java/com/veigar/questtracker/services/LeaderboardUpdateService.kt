package com.veigar.questtracker.services

import android.util.Log
import com.veigar.questtracker.data.LeaderboardsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.ChildData
import com.veigar.questtracker.model.LeaderboardModel
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object LeaderboardUpdateService {
    private const val TAG = "LeaderboardUpdateService"

    /**
     * Updates the leaderboard data for the current parent user
     * This can be called from anywhere in the app without requiring the leaderboards screen
     */
    fun updateLeaderboardForParent(
        parentUser: UserModel,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            try {
                Log.d(TAG, "Starting automatic leaderboard update for parent: ${parentUser.getDecodedUid()}")
                
                val childList = mutableListOf<ChildData>()
                val lastUpdated = System.currentTimeMillis()
                
                // Get linked children
                val children = UserRepository.getLinkedChildren().getOrNull()
                Log.d(TAG, "Found ${children?.size ?: 0} linked children")
                
                if (children != null) {
                    for (child in children) {
                        Log.d(TAG, "Processing child: ${child.getDecodedUid()}")
                        
                        // Get child's completed tasks
                        val tasks = LeaderboardsRepository.getChildClaimedTasks(child.getDecodedUid())
                        Log.d(TAG, "Found ${tasks.size} completed tasks for child ${child.getDecodedUid()}")
                        
                        // Calculate task counts for different periods
                        val (daily, weekly, monthly) = calculateTaskCounts(tasks)
                        
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
                        childList.add(childData)
                        Log.d(TAG, "Added child data: $childData")
                    }
                }
                
                // Create leaderboard model
                val leaderboardModel = LeaderboardModel(
                    leaderboardId = parentUser.getDecodedUid(),
                    parentModel = parentUser,
                    childList = childList,
                    lastUpdated = lastUpdated
                )
                
                // Upload to Firebase
                val result = LeaderboardsRepository.uploadLeaderboardData(leaderboardModel)
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully updated leaderboard for parent: ${parentUser.getDecodedUid()}")
                } else {
                    Log.e(TAG, "Failed to update leaderboard: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating leaderboard for parent: ${parentUser.getDecodedUid()}", e)
            }
        }
    }

    /**
     * Calculates daily, weekly, and monthly task counts for a child
     */
    private fun calculateTaskCounts(tasks: List<com.veigar.questtracker.model.TaskModel>): Triple<Int, Int, Int> {
        var daily = 0
        var weekly = 0
        var monthly = 0
        
        val today = Calendar.getInstance()
        
        for (task in tasks) {
            val taskCompletionStatus = task.completedStatus
            val completedAtTimestamp = taskCompletionStatus?.completedAt
            
            if (completedAtTimestamp != null) {
                val dateCompleted = Calendar.getInstance().apply { 
                    timeInMillis = completedAtTimestamp 
                }
                
                // Check if completed today
                if (isSameDay(dateCompleted, today)) {
                    daily++
                }
                
                // Check if completed this week
                if (isSameWeek(dateCompleted, today)) {
                    weekly++
                }
                
                // Check if completed this month
                if (isSameMonth(dateCompleted, today)) {
                    monthly++
                }
            }
        }
        
        return Triple(daily, weekly, monthly)
    }

    /**
     * Checks if two Calendar instances represent the same day
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if two Calendar instances are in the same week
     */
    private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
        val startOfWeek = Calendar.getInstance().apply {
            time = cal2.time
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        val endOfWeek = Calendar.getInstance().apply {
            time = startOfWeek.time
            add(Calendar.DAY_OF_WEEK, 6)
        }
        
        return (cal1.after(startOfWeek) && cal1.before(endOfWeek)) ||
               cal1.equals(startOfWeek) || cal1.equals(endOfWeek)
    }

    /**
     * Checks if two Calendar instances are in the same month
     */
    private fun isSameMonth(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    /**
     * Updates leaderboard for current logged-in parent user
     */
    fun updateCurrentParentLeaderboard(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            try {
                val currentUser = UserRepository.getUserProfile()
                if (currentUser != null && currentUser.role == "parent") {
                    updateLeaderboardForParent(currentUser, scope)
                } else {
                    Log.d(TAG, "Current user is not a parent, skipping leaderboard update")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current user for leaderboard update", e)
            }
        }
    }
}



