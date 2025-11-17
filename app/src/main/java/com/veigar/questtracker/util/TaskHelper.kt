package com.veigar.questtracker.util

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.veigar.questtracker.model.RepeatFrequency
import com.veigar.questtracker.model.TaskModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

import java.time.DayOfWeek as JavaTimeDayOfWeek

@RequiresApi(Build.VERSION_CODES.O)
internal fun parseDate(dateString: String?): LocalDate? { // Changed to internal
    if (dateString.isNullOrBlank()) return null
    return try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        Log.e("DateHelper", "Error parsing date: $dateString", e)
        null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun isTaskActive(task: TaskModel, today: LocalDate): Boolean { // Changed to internal
    val startDate = parseDate(task.startDate)
    val endDate = parseDate(task.endDate)

    if (startDate != null && today.isBefore(startDate)) {
        return false
    }
    if (endDate != null && today.isAfter(endDate)) {
        return false
    }
    return true
}

// For the extension function, if it remains in this util file:
// You'll call it like: modelDayOfWeek.toJavaTimeDayOfWeek() after importing the extension.
// OR make it a regular function:
// internal fun convertModelDayOfWeekToJavaTimeDayOfWeek(modelDay: com.veigar.questtracker.model.DayOfWeek): JavaTimeDayOfWeek {
// return JavaTimeDayOfWeek.valueOf(modelDay.name)
// }
// Let's assume you keep it as an extension for now.
@RequiresApi(Build.VERSION_CODES.O)
internal fun com.veigar.questtracker.model.DayOfWeek.toJavaTimeDayOfWeek(): JavaTimeDayOfWeek { // Changed to internal
    return JavaTimeDayOfWeek.valueOf(this.name)
}


@RequiresApi(Build.VERSION_CODES.O)
internal fun isDailyTaskDue(task: TaskModel, today: LocalDate): Boolean { // Changed to internal
    // ... (rest of the function body)
    if (task.repeat?.frequency != RepeatFrequency.DAILY || !isTaskActive(task, today)) {
        return false
    }

    val taskStartDate = parseDate(task.startDate) ?: return false

    val interval = task.repeat.interval.takeIf { it > 0 } ?: 1
    val daysBetween = ChronoUnit.DAYS.between(taskStartDate, today)

    return daysBetween >= 0 && (daysBetween % interval == 0L)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun isWeeklyTaskDueThisWeek(task: TaskModel, today: LocalDate): Boolean { // Changed to internal
    // ... (rest of the function body)
    if (task.repeat?.frequency != RepeatFrequency.WEEKLY || !isTaskActive(task, today)) {
        return false
    }
    val taskStartDate = parseDate(task.startDate) ?: return false

    val intervalWeeks = task.repeat.interval.takeIf { it > 0 } ?: 1
    val weeksSinceStart = ChronoUnit.WEEKS.between(taskStartDate, today)

    if (weeksSinceStart < 0 || (weeksSinceStart % intervalWeeks != 0L)) {
        return false
    }

    if (task.repeat.weeklyDays.isEmpty()) {
        return true
    }

    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(JavaTimeDayOfWeek.MONDAY))
    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(JavaTimeDayOfWeek.SUNDAY))

    // Ensure you import your DayOfWeek's extension function or call it appropriately
    val dueDaysInJavaTime = task.repeat.weeklyDays.map { it.toJavaTimeDayOfWeek() }

    var nextPotentialDueDate = taskStartDate
    while(nextPotentialDueDate.dayOfWeek !in dueDaysInJavaTime || nextPotentialDueDate.isBefore(taskStartDate)) {
        if (nextPotentialDueDate.dayOfWeek in dueDaysInJavaTime && !nextPotentialDueDate.isBefore(taskStartDate)) break
        nextPotentialDueDate = nextPotentialDueDate.plusDays(1)
        if (nextPotentialDueDate.isAfter(taskStartDate.plusYears(5))) return false
    }

    val weeksFromFirstValidOccurrence = ChronoUnit.WEEKS.between(nextPotentialDueDate, today)
    if (weeksFromFirstValidOccurrence < 0 || weeksFromFirstValidOccurrence % intervalWeeks != 0L) {
        return false
    }

    for (dueDay in dueDaysInJavaTime) {
        var currentTestDate = startOfWeek
        while (!currentTestDate.isAfter(endOfWeek)) {
            if (currentTestDate.dayOfWeek == dueDay && !currentTestDate.isBefore(today)) {
                if (!currentTestDate.isBefore(taskStartDate)) return true
            }
            currentTestDate = currentTestDate.plusDays(1)
        }
    }
    return false
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun getNextResetInfo(task: TaskModel): String {
    val today = LocalDate.now()
    if (!isTaskActive(task, today)) return "Task not active"

    when (task.repeat?.frequency) {
        RepeatFrequency.DAILY -> {
            val now = LocalDateTime.now()
            val endOfDay = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX)
            val hoursRemaining = ChronoUnit.HOURS.between(now, endOfDay)
            return "$hoursRemaining h"
        }
        RepeatFrequency.WEEKLY -> {
            if (task.repeat.weeklyDays.isEmpty()) return "No specific reset day" // Or handle as an error

            val dueDaysInJavaTime = task.repeat.weeklyDays.map { it.toJavaTimeDayOfWeek() }
            var nextResetDate = today.plusDays(1) // Start checking from tomorrow

            for (i in 0..7) { // Check the next 7 days
                if (nextResetDate.dayOfWeek in dueDaysInJavaTime) {
                    // Check if this reset day aligns with the interval
                    if (isWeeklyTaskDueThisWeek(task, nextResetDate) || isWeeklyTaskDueToday(task.copy(startDate = parseDate(task.startDate).toString()), nextResetDate)) {
                        return "${nextResetDate.dayOfWeek}"
                    }
                }
                nextResetDate = nextResetDate.plusDays(1)
            }
            return "Next reset not found in near future" // Should ideally find one if task is active and repeats
        }
        else -> return "Task doesn't repeat"
    }
}
@RequiresApi(Build.VERSION_CODES.O)
internal fun isWeeklyTaskDueToday(task: TaskModel, today: LocalDate): Boolean { // Changed to internal
    // ... (rest of the function body)
    if (!isWeeklyTaskDueThisWeek(task, today)) return false

    val taskStartDate = parseDate(task.startDate) ?: return false
    if (task.repeat!!.weeklyDays.isEmpty()) {
        return taskStartDate.dayOfWeek == today.dayOfWeek
    }
    // Ensure you import your DayOfWeek's extension function or call it appropriately
    return task.repeat.weeklyDays.any { it.toJavaTimeDayOfWeek() == today.dayOfWeek }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun isOneTimeTaskDue(task: TaskModel, today: LocalDate): Boolean { // Changed to internal
    // ... (rest of the function body)
    if (task.repeat != null && (task.repeat.frequency == RepeatFrequency.DAILY || task.repeat.frequency == RepeatFrequency.WEEKLY)) {
        return false
    }

    if (!isTaskActive(task, today)) return false

    val dueDate = parseDate(task.endDate)
    return dueDate != null && dueDate.isEqual(today)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun isTaskUpcoming(task: TaskModel, today: LocalDate): Boolean { // Changed to internal
    // ... (rest of the function body)
    val taskStartDate = parseDate(task.startDate)
    val taskEndDate = parseDate(task.endDate)

    if (taskStartDate != null && today.isBefore(taskStartDate)) {
        return true
    }

    if (!isTaskActive(task, today)) {
        return false
    }

    if (isDailyTaskDue(task, today)) return false
    if (isWeeklyTaskDueToday(task, today)) return false
    if (task.repeat?.frequency == RepeatFrequency.WEEKLY && isWeeklyTaskDueThisWeek(task, today) && !isWeeklyTaskDueToday(task,today)) {
        return true
    }

    if (task.repeat == null || (task.repeat.frequency != RepeatFrequency.DAILY && task.repeat.frequency != RepeatFrequency.WEEKLY) ) {
        val dueDate = parseDate(task.endDate)
        if (dueDate != null && dueDate.isAfter(today)) {
            return true
        }
        if (dueDate == null && isTaskActive(task, today) && !isOneTimeTaskDue(task, today)) return true
    }

    var nextPotentialDueDate: LocalDate? = null
    when (task.repeat?.frequency) {
        RepeatFrequency.DAILY -> {
            if (!isDailyTaskDue(task, today) && taskStartDate != null) {
                val interval = task.repeat.interval.takeIf { it > 0 } ?: 1
                var tempDate = today.plusDays(1)
                for (i in 0..365) {
                    if (isDailyTaskDue(task.copy(startDate = taskStartDate.toString()), tempDate)) {
                        nextPotentialDueDate = tempDate
                        break
                    }
                    tempDate = tempDate.plusDays(1)
                }
            }
        }
        RepeatFrequency.WEEKLY -> {
            if (!isWeeklyTaskDueThisWeek(task, today) && taskStartDate != null) {
                var tempDate = today.with(TemporalAdjusters.next(JavaTimeDayOfWeek.MONDAY))
                for (i in 0..52) {
                    if(isWeeklyTaskDueThisWeek(task.copy(startDate = taskStartDate.toString()), tempDate) || isWeeklyTaskDueToday(task.copy(startDate = taskStartDate.toString()), tempDate) ){
                        nextPotentialDueDate = tempDate
                        break
                    }
                    tempDate = tempDate.plusWeeks(1)
                }
            }
        }
        null -> { /* Handled */ }
    }

    if (nextPotentialDueDate != null) {
        if (taskEndDate != null && nextPotentialDueDate.isAfter(taskEndDate)) {
            return false
        }
        return true
    }
    return false
}

