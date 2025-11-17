package com.veigar.questtracker.ui.component.createtask

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // For selected state
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.DayOfWeek // Make sure this enum exists
import com.veigar.questtracker.model.RepeatFrequency // Make sure this enum exists
import java.util.Locale


val cfPrimarySelectedColor = Color(0xFF66BB6A)
val cfAccentSelectedColor = Color(0xFF66BB6A)  // A cheerful Amber/Orange

// Define colors for text when on top of these selected backgrounds
val cfTextOnPrimarySelected = Color.White
val cfTextOnAccentSelected = Color.Black.copy(alpha = 0.85f) // Good for lighter accents like Amber/Orange
// Change to White if accent is darker

// Define colors for unselected states (subtle, to work on a dark main background)
val cfUnselectedChipBg = Color.White.copy(alpha = 0.15f)
val cfUnselectedChipBorder = Color.White.copy(alpha = 0.4f)
val cfUnselectedChipText = Color.White // Text for unselected chips will be white

enum class DailyFrequency {
    ONCE,
    TWICE,
    THRICE
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RepeatTaskSection(
    isRepeating: Boolean,
    onRepeatingChange: (Boolean) -> Unit,
    repeatFrequency: RepeatFrequency,
    onFrequencyChange: (RepeatFrequency) -> Unit,
    repeatInterval: Int,
    onIntervalChange: (Int) -> Unit,
    selectedDays: List<DayOfWeek>,
    onSelectedDaysChange: (List<DayOfWeek>) -> Unit,
    dailyFrequency: DailyFrequency,
    onDailyFrequencyChange: (DailyFrequency) -> Unit,
    hourlyInterval: Int,
    onHourlyIntervalChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    val sectionTextColor = Color.White // Main text color for this section

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Make this a Repeating Quest?",
                style = MaterialTheme.typography.titleMedium,
                color = sectionTextColor
            )
            Switch(
                checked = isRepeating,
                onCheckedChange = { if (enabled) onRepeatingChange(it) },
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cfPrimarySelectedColor,
                    checkedTrackColor = cfPrimarySelectedColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }

        AnimatedVisibility(visible = isRepeating) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "Repeat daily or weekly?",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = sectionTextColor,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RepeatFrequency.entries.forEach { freq ->
                        FrequencyChip( // Updated to use new color scheme
                            text = freq.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                            isSelected = repeatFrequency == freq,
                            onClick = { onFrequencyChange(freq) }
                        )
                    }
                }

                if (repeatFrequency == RepeatFrequency.DAILY) {
                    Text(
                        "How many times a day?",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = sectionTextColor,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DailyFrequency.entries.forEach { freq ->
                            FrequencyChip(
                                text = freq.name.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                isSelected = dailyFrequency == freq,
                                onClick = { onDailyFrequencyChange(freq) }
                            )
                        }
                    }

                    if (dailyFrequency == DailyFrequency.TWICE || dailyFrequency == DailyFrequency.THRICE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                "Repeats every",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = sectionTextColor,
                                modifier = Modifier.alignByBaseline()
                            )
                            Spacer(Modifier.width(12.dp))
                            LabeledIntField(
                                value = hourlyInterval,
                                onValueChange = onHourlyIntervalChange,
                                modifier = Modifier
                                    .width(70.dp)
                                    .alignByBaseline(),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (hourlyInterval == 1) "hour" else "hours",
                                modifier = Modifier.alignByBaseline(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = sectionTextColor
                            )
                        }
                    }
                } else if (repeatFrequency == RepeatFrequency.WEEKLY) {
                    Text(
                        "On these days of the week:",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = sectionTextColor,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    DaySelector( // Updated to use new color scheme
                        allDays = DayOfWeek.entries.toList(),
                        selectedDays = selectedDays,
                        onDayClick = { day ->
                            val newSelectedDays = selectedDays.toMutableList()
                            if (newSelectedDays.contains(day)) {
                                newSelectedDays.remove(day)
                            } else {
                                newSelectedDays.add(day)
                            }
                            onSelectedDaysChange(newSelectedDays)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) cfPrimarySelectedColor else cfUnselectedChipBg
    val textColor = if (isSelected) cfTextOnPrimarySelected else cfUnselectedChipText
    val border = if (isSelected) null else BorderStroke(1.dp, cfUnselectedChipBorder)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = textColor, // This will be white (cfTextOnPrimarySelected)
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DaySelector(
    allDays: List<DayOfWeek>,
    selectedDays: List<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        allDays.forEach { day ->
            val isSelected = selectedDays.contains(day)
            FilterChip(
                selected = isSelected,
                onClick = { onDayClick(day) },
                label = {
                    Text(
                        day.name.first().toString(),
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                },
                modifier = Modifier.padding(horizontal = 1.dp),
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = cfUnselectedChipBg,
                    selectedContainerColor = cfAccentSelectedColor, // Using accent for days
                    labelColor = cfUnselectedChipText, // White text on unselected
                    selectedLabelColor = cfTextOnAccentSelected // Black (or white if accent is dark) on selected
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = cfUnselectedChipBorder,
                    selectedBorderColor = cfAccentSelectedColor.copy(alpha = 0.7f), // Or Color.Transparent
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp,
                    enabled = false,
                    selected = false
                    // enabled and selected states for border are handled by default,
                    // or you can explicitly set selectedBorderColor, disabledSelectedBorderColor etc.
                ),
                elevation = FilterChipDefaults.filterChipElevation(
                    elevation = 1.dp,
                    pressedElevation = 3.dp,
                )
            )
        }
    }
}
