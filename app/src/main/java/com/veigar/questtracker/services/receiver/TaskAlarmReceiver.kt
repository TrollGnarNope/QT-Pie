package com.veigar.questtracker.services.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.veigar.questtracker.R

class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.veigar.questtracker.ACTION_TASK_REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        private const val TASK_REMINDER_NOTIFICATION_CHANNEL_ID = "TaskReminderChannel"
        private const val TAG = "TaskAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "TaskAlarmReceiver received intent: ${intent.action}")
        if (intent.action == ACTION_TASK_REMINDER) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"

            if (taskId == null) {
                Log.e(TAG, "Task ID is missing in reminder intent.")
                return
            }

            Log.i(TAG, "Displaying reminder for task: $taskTitle (ID: $taskId)")
            showTaskReminderNotification(context, taskId, taskTitle)
        }
    }

    private fun showTaskReminderNotification(context: Context, taskId: String, taskTitle: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TASK_REMINDER_NOTIFICATION_CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH // Reminders should be high importance
            ).apply {
                description = "Shows individual task reminders."
                // Configure sound, vibration, etc. as needed
            }
            notificationManager.createNotificationChannel(channel)
        }

        // The notification will just be informational, not opening the app directly.
        // If you want an action, you'd define an Intent and PendingIntent here.
        // For now, setting contentIntent to null or not setting it means tapping does nothing.
        // val pendingIntent: PendingIntent? = null // Explicitly null if no action

        val notification = NotificationCompat.Builder(context, TASK_REMINDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.to_do_list) // Specific icon for alarm/reminder
            .setContentTitle(taskTitle)
            .setContentText("It's time for your task!") // Or more specific details
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // .setContentIntent(pendingIntent) // Comment out or remove to disable tap action
            .setAutoCancel(true) // Dismiss the notification when tapped
            .build()

        // Use a unique notification ID for each task reminder,
        // so multiple reminders can appear if scheduled close together.
        // taskId.hashCode() can be a good candidate if task IDs are sufficiently unique.
        notificationManager.notify(taskId.hashCode(), notification)
    }
}
