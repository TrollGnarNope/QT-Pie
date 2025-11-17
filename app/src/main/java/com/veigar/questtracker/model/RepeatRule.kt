package com.veigar.questtracker.model

import kotlinx.serialization.Serializable

@Serializable
data class RepeatRule(
    val frequency: RepeatFrequency = RepeatFrequency.DAILY, // NONE, DAILY, WEEKLY
    val interval: Int = 1, // every N days/weeks
    val weeklyDays: List<DayOfWeek> = emptyList(), // for weekly tasks
    val dailyFrequency: String = "ONCE",
    val hourlyInterval: Int = 0
) {
    override fun toString(): String {
        return when (frequency) {
            RepeatFrequency.DAILY -> "Every $interval ${frequency.getDisplayName()}(s)"
            RepeatFrequency.WEEKLY -> {
                val daysString = if (weeklyDays.isNotEmpty()) " on ${weeklyDays.joinToString { it.getDisplayName() }}" else "?"
                "Every $interval ${frequency.getDisplayName()}(s)$daysString"
            }
        }
    }
}

@Serializable
enum class RepeatFrequency {
    DAILY, WEEKLY;
    fun getDisplayName(): String {
        return when (this) {
            DAILY -> "Day"
            WEEKLY -> "Week"
        }
    }
}

@Serializable
enum class DayOfWeek {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    ;

    fun getDisplayName(): String {
        return when (this) {
            SUNDAY -> "Sunday"
            MONDAY -> "Monday"
            TUESDAY -> "Tuesday"
            WEDNESDAY -> "Wednesday"
            THURSDAY -> "Thursday"
            FRIDAY -> "Friday"
            SATURDAY -> "Saturday"
        }
    }
}