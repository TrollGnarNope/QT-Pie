package com.veigar.questtracker.ui.component.rewards

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.veigar.questtracker.ui.theme.CoralBlueDark


val SoftBlue = Color(0xFFB3E5FC)
val SoftGreen = CoralBlueDark
val SoftYellow = Color(0xFFFFF59D)
val TextColorPrimary = Color(0xFF424242)
val TextColorSecondary = Color(0xFF757575)
val ErrorColorSoft = Color(0xFFEF9A9A)
val InfoColor = Color(0xFF2196F3)

@SuppressLint("UnrememberedMutableState")
@Composable
fun AddRewardDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, points: Int, description: String?, requiresApproval: Boolean, quantityLimit: Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var pointsString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var pointsError by remember { mutableStateOf<String?>(null) }
    var quantityLimitError by remember { mutableStateOf<String?>(null) }

    var quantityLimitString by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val hasQuantityLimit = quantityLimitString.isNotBlank() && quantityLimitString.toIntOrNull() != null && quantityLimitString.toIntOrNull()!! > 0
    var requiresApproval by remember { mutableStateOf(false) }

    LaunchedEffect(hasQuantityLimit) {
        if (hasQuantityLimit) {
            requiresApproval = true
        }
    }

    val isFormValid by derivedStateOf {
        title.isNotBlank() &&
                pointsString.toIntOrNull() != null && pointsString.toIntOrNull()!! > 0 &&
                (quantityLimitString.isEmpty() || (quantityLimitString.toIntOrNull() != null && quantityLimitString.toIntOrNull()!! > 0)) &&
                titleError == null && pointsError == null && quantityLimitError == null
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = SoftBlue.copy(alpha = 0.98f))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Create a Reward!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextColorPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = if (it.isBlank()) "What's the reward called?" else null
                    },
                    label = { Text("Reward Name*", color = TextColorSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = {
                        if (titleError != null) Text(titleError!!, color = ErrorColorSoft, fontSize = 11.sp)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        errorBorderColor = ErrorColorSoft,
                        focusedLabelColor = SoftGreen,
                        errorLabelColor = ErrorColorSoft,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                // Points Field
                OutlinedTextField(
                    value = pointsString,
                    onValueChange = { value ->
                        pointsString = value.filter { it.isDigit() }
                        pointsError = when {
                            value.isBlank() -> "How many points?"
                            value.toIntOrNull() == null -> "Oops, numbers only!"
                            value.toIntOrNull()!! <= 0 -> "Needs at least 1 point!"
                            else -> null
                        }
                    },
                    label = { Text("Points Needed*", color = TextColorSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = "Points", tint = SoftYellow, modifier = Modifier.size(20.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = pointsError != null,
                    supportingText = {
                        if (pointsError != null) Text(pointsError!!, color = ErrorColorSoft, fontSize = 11.sp)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        errorBorderColor = ErrorColorSoft,
                        focusedLabelColor = SoftGreen,
                        errorLabelColor = ErrorColorSoft,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                // Quantity Limit Field
                OutlinedTextField(
                    value = quantityLimitString,
                    onValueChange = { value ->
                        val newQuantity = value.filter { it.isDigit() }
                        quantityLimitString = newQuantity
                        quantityLimitError = when {
                            newQuantity.isNotEmpty() && newQuantity.toIntOrNull() == null -> "Numbers only!"
                            newQuantity.isNotEmpty() && newQuantity.toIntOrNull()!! <= 0 -> "Must be > 0 or empty"
                            else -> null
                        }
                    },
                    label = { Text("Quantity? (Optional)", color = TextColorSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Empty for 1 per child", color = TextColorSecondary.copy(alpha=0.7f), fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = quantityLimitError != null,
                    supportingText = {
                        if (quantityLimitError != null) Text(quantityLimitError!!, color = ErrorColorSoft, fontSize = 11.sp)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        errorBorderColor = ErrorColorSoft,
                        focusedLabelColor = SoftGreen,
                        errorLabelColor = ErrorColorSoft,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = TextColorSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 100.dp),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        focusedLabelColor = SoftGreen,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )


                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                enabled = !hasQuantityLimit,
                                onClick = {
                                    if (!hasQuantityLimit) requiresApproval = !requiresApproval
                                }
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Checkbox(
                            checked = requiresApproval,
                            onCheckedChange = {
                                if (!hasQuantityLimit) requiresApproval = it
                            },
                            enabled = !hasQuantityLimit,
                            colors = CheckboxDefaults.colors(
                                checkedColor = SoftGreen,
                                uncheckedColor = TextColorSecondary,
                                checkmarkColor = Color.White,
                                disabledCheckedColor = SoftGreen.copy(alpha = 0.6f),
                                disabledUncheckedColor = TextColorSecondary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Accept/Reject?",
                            color = if (!hasQuantityLimit) TextColorPrimary else TextColorSecondary.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AnimatedVisibility(
                        visible = hasQuantityLimit,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, top = 0.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Information",
                                tint = InfoColor, // Your InfoColor
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Approval required if quantity is set.",
                                style = MaterialTheme.typography.labelSmall,
                                color = InfoColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = TextColorSecondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Later", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            titleError = if (title.isBlank()) "What's the reward called?" else null
                            val points = pointsString.toIntOrNull()
                            pointsError = when {
                                pointsString.isBlank() -> "How many points?"
                                points == null -> "Oops, numbers only!"
                                points <= 0 -> "Needs at least 1 point!"
                                else -> null
                            }
                            val quantityLimitValue = quantityLimitString.toIntOrNull()
                            quantityLimitError = when {
                                quantityLimitString.isNotEmpty() && quantityLimitValue == null -> "Numbers only!"
                                quantityLimitString.isNotEmpty() && quantityLimitValue != null && quantityLimitValue <= 0 -> "Must be > 0 or empty"
                                else -> null
                            }

                            if (isFormValid && points != null) {
                                onConfirm(
                                    title.trim(),
                                    points,
                                    description.trim().ifBlank { null },
                                    requiresApproval,
                                    if (quantityLimitString.isBlank()) null else quantityLimitValue
                                )
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGreen,
                            contentColor = Color.White,
                            disabledContainerColor = TextColorSecondary.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 2.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Add Reward", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
