package com.veigar.questtracker.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.services.receiver.TaskAlarmReceiver
import java.util.*

object TaskAlarmScheduler {

    private const val TAG = "TaskAlarmScheduler"

    // Helper function to convert "HH:mm" string to the next valid trigger time in millis
    private fun calculateNextTriggerTimeMillis(reminderTimeString: String): Long? {
        return try {
            val parts = reminderTimeString.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            if (hour !in 0..23 || minute !in 0..59) return null // Basic validation

            val now = Calendar.getInstance()
            val reminderTimeToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If reminder time today has already passed, schedule for tomorrow
            if (reminderTimeToday.before(now)) {
                reminderTimeToday.add(Calendar.DAY_OF_YEAR, 1)
            }
            Log.d(TAG, "Calculated trigger time for $reminderTimeString: ${Date(reminderTimeToday.timeInMillis)}")
            reminderTimeToday.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reminderTime string: $reminderTimeString", e)
            null
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskModel) {
        if (task.reminderTime.isNullOrBlank()) {
            Log.d(TAG, "Skipping alarm for task '${task.title}': No reminderTime or task completed.")
            cancelTaskReminder(context, task) // Ensure any old alarm is cancelled
            return
        }

        val triggerAtMillis = calculateNextTriggerTimeMillis(task.reminderTime)

        if (triggerAtMillis == null || triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping alarm for task '${task.title}': Invalid time format or calculated time is in the past.")
            cancelTaskReminder(context, task)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.taskId)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
            // data = Uri.parse("task://${task.id}") // Optional: makes intent more unique
        }

        val pendingIntentRequestCode = task.taskId.hashCode()
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pendingIntentRequestCode,
            intent,
            pendingIntentFlags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms for task '${task.title}'. App needs SCHEDULE_EXACT_ALARM permission or user setting enabled.")
                // Fallback or request permission.
                // As a fallback, you could schedule an inexact alarm:
                // alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                // Or inform the user. For now, we just skip scheduling the exact one.
                return
            }

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.i(TAG, "Alarm scheduled for task '${task.title}' (ID: ${task.taskId}) at ${Date(triggerAtMillis)} (raw: $triggerAtMillis)")

        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException for task '${task.title}': Missing SCHEDULE_EXACT_ALARM or app restricted.", se)
        }
    }

    // cancelTaskReminder remains the same as before

    fun cancelTaskReminder(context: Context, task: TaskModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
        }

        val pendingIntentRequestCode = task.taskId.hashCode()
        val pendingIntentFlags =
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            pendingIntentRequestCode,
            intent,
            pendingIntentFlags
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled alarm for task '${task.title}' (ID: ${task.taskId})")
        } else {
            Log.d(TAG, "No alarm found to cancel for task '${task.title}' (ID: ${task.taskId})")
        }
    }


    // scheduleRemindersForAllTasks remains the same
    fun scheduleRemindersForAllTasks(context: Context, tasks: List<TaskModel>) {
        Log.d(TAG, "Scheduling/Updating reminders for ${tasks.size} tasks based on HH:mm.")
        tasks.forEach { task ->
            scheduleTaskReminder(context, task)
        }
    }

    // cancelAllTaskReminders remains the same
    fun cancelAllTaskReminders(context: Context, tasks: List<TaskModel>?) {
        tasks?.forEach { task ->
            cancelTaskReminder(context, task)
        }
        Log.i(TAG, "Attempted to cancel all task reminders (if tasks list provided).")
    }
}
