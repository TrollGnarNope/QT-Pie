package com.veigar.questtracker.ui.component.createtask

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

// Define child-friendly colors for row backgrounds (USING SOFT LAVENDER)
val scheduleRowColors = listOf(
    Color(0xFFCE93D8).copy(alpha = 0.4f), // Soft Lavender
    Color(0xFFA5D6A7).copy(alpha = 0.4f), // Light Green
    Color(0xFFFFF59D).copy(alpha = 0.6f)  // Pale Yellow
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleSection(
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    reminderTime: LocalTime?,
    onReminderTimeChange: (LocalTime?) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val startDatePickerDialog = DatePickerDialog(
        context, { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            onStartDateChange(LocalDate.of(year, month + 1, dayOfMonth))
        }, startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
    )
    startDatePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
        clear()
        set(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
    }.timeInMillis

    val endDatePickerDialog = DatePickerDialog(
        context, { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            onEndDateChange(LocalDate.of(year, month + 1, dayOfMonth))
        }, endDate?.year ?: startDate.year, endDate?.monthValue?.minus(1) ?: startDate.monthValue - 1, endDate?.dayOfMonth ?: startDate.dayOfMonth
    )
    endDatePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
        clear()
        set(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
    }.timeInMillis

    val initialHour = reminderTime?.hour ?: 9
    val initialMinute = reminderTime?.minute ?: 0
    val timePickerDialog = TimePickerDialog(
        context, { _, hour: Int, minute: Int ->
            onReminderTimeChange(LocalTime.of(hour, minute))
        }, initialHour, initialMinute, false
    )

    val iconColor = Color.White.copy(alpha = 0.85f)
    val clearIconColor = Color.White.copy(alpha = 0.7f)
    val optionalTextColor = Color.White.copy(alpha = 0.7f)

    Column {
        Text(
            "Quest Scheduling",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
        )

        ScheduleItemRow(
            label = "Starts on:",
            value = startDate.format(dateFormatter),
            icon = Icons.Filled.DateRange,
            iconColor = iconColor,
            textColor = Color.White,
            backgroundColor = scheduleRowColors[0], // First color (Lavender)
            onClick = { startDatePickerDialog.show() }
        )

        ScheduleItemRow(
            label = "Ends on:",
            value = endDate?.format(dateFormatter) ?: "Not set",
            icon = Icons.Filled.DateRange,
            iconColor = iconColor,
            textColor = if (endDate != null) Color.White else optionalTextColor,
            backgroundColor = scheduleRowColors[1], // Second color (Green)
            onClick = { endDatePickerDialog.show() },
            onClearClick = if (endDate != null) { { onEndDateChange(null) } } else null,
            clearIconColor = clearIconColor
        )

        ScheduleItemRow(
            label = "Remind at:",
            value = reminderTime?.format(timeFormatter) ?: "Not set",
            icon = Icons.Filled.Schedule,
            iconColor = iconColor,
            textColor = if (reminderTime != null) Color.White else optionalTextColor,
            backgroundColor = scheduleRowColors[2], // Third color (Yellow)
            onClick = { timePickerDialog.show() },
            onClearClick = if (reminderTime != null) { { onReminderTimeChange(null) } } else null,
            clearIconColor = clearIconColor
        )
    }
}

// ScheduleItemRow composable remains the same as in the previous good response
@Composable
fun ScheduleItemRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    textColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    onClearClick: (() -> Unit)? = null,
    clearIconColor: Color = Color.White.copy(alpha = 0.7f)
) {
    val rowShape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(rowShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = textColor
            )
        }

        if (onClearClick != null) {
            IconButton(
                onClick = onClearClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = "Clear ${label.removeSuffix(":")}",
                    tint = clearIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(Modifier.width(36.dp))
        }
    }
}