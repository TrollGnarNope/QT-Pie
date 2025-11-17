package com.veigar.questtracker.services

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.veigar.questtracker.model.TaskModel
import androidx.core.content.edit

class TaskReminder {
    companion object {
        private const val PREFS_NAME = "QuestTrackerPrefs"
        private const val TASKS_KEY = "Tasks"

        fun saveTasks(context: Context, tasks: List<TaskModel>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                val gson = Gson()
                val json = gson.toJson(tasks)
                putString(TASKS_KEY, json)
            }
            Log.d("TaskReminder", "Tasks saved: $tasks")
            TaskAlarmScheduler.scheduleRemindersForAllTasks(context.applicationContext, tasks)
        }

        fun loadTasks(context: Context): List<TaskModel> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            val json = prefs.getString(TASKS_KEY, null)
            val type = object : TypeToken<List<TaskModel>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        }
    }
}

data class TaskNotificationContent(
    val contentText: String, // For the collapsed view
    val style: NotificationCompat.Style?, // InboxStyle or BigTextStyle or null
    val hasTasks: Boolean
)

class DailyTaskNotifier(private val context: Context) {

    companion object {

    }

    // This method prepares the task-specific parts for the notification
    fun getTaskContentForNotification(): TaskNotificationContent {
        val tasks: List<TaskModel> = TaskReminder.loadTasks(context)
        if (tasks.isEmpty()) {
            return TaskNotificationContent(
                contentText = "No active tasks for today.",
                style = null,
                hasTasks = false
            )
        }

        val collapsedText = "${tasks.size} task(s) for today."
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("DAILY TASKS") // Title for the expanded view

        tasks.take(7).forEach { task -> // Show max 7 lines in expanded view
            inboxStyle.addLine("• ${task.title}")
        }
        if (tasks.size > 7) {
            inboxStyle.addLine("  ...and ${tasks.size - 7} more.")
        }
        inboxStyle.setSummaryText("${tasks.size} active task(s)") // Summary for expanded view

        return TaskNotificationContent(
            contentText = collapsedText,
            style = inboxStyle,
            hasTasks = true
        )
    }
}
