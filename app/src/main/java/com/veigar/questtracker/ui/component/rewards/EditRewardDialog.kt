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
import com.veigar.questtracker.model.RewardModel


@SuppressLint("UnrememberedMutableState")
@Composable
fun EditRewardDialog(
    rewardToEdit: RewardModel,
    onDismissRequest: () -> Unit,
    onConfirmEdit: (updatedReward: RewardModel) -> Unit
) {
    var title by remember { mutableStateOf(rewardToEdit.title) }
    var pointsString by remember { mutableStateOf(rewardToEdit.pointsRequired.toString()) }
    var description by remember { mutableStateOf(rewardToEdit.description ?: "") }
    var quantityLimitString by remember { mutableStateOf(rewardToEdit.quantityLimit?.toString() ?: "") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var pointsError by remember { mutableStateOf<String?>(null) }
    var quantityLimitError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    val hasQuantityLimit = quantityLimitString.isNotBlank() &&
            quantityLimitString.toIntOrNull() != null &&
            quantityLimitString.toIntOrNull()!! > 0

    var requiresApproval by remember { mutableStateOf(rewardToEdit.requiresApproval) }

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
            shape = RoundedCornerShape(20.dp), // Compact: Smaller corner radius
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = SoftBlue.copy(alpha = 0.98f)) // Consistent with AddDialog
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp) // Compact: Reduced padding
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp) // Compact: Reduced spacing
            ) {
                Text(
                    "Edit Reward",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), // Compact: Smaller headline
                    color = TextColorPrimary,
                    modifier = Modifier.padding(bottom = 6.dp) // Compact: Reduced bottom padding
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = if (it.isBlank()) "Reward name can't be empty" else null
                    },
                    label = { Text("Reward Name*", color = TextColorSecondary, fontSize = 13.sp) }, // Compact
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = {
                        if (titleError != null) Text(titleError!!, color = ErrorColorSoft, fontSize = 11.sp) // Compact
                    },
                    shape = RoundedCornerShape(12.dp), // Compact
                    colors = OutlinedTextFieldDefaults.colors( // Restored full colors
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
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp) // Compact
                )

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
                    label = { Text("Points Needed*", color = TextColorSecondary, fontSize = 13.sp) }, // Compact
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = "Points", tint = SoftYellow, modifier = Modifier.size(20.dp)) }, // Compact
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = pointsError != null,
                    supportingText = {
                        if (pointsError != null) Text(pointsError!!, color = ErrorColorSoft, fontSize = 11.sp) // Compact
                    },
                    shape = RoundedCornerShape(12.dp), // Compact
                    colors = OutlinedTextFieldDefaults.colors( // Restored full colors
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        errorBorderColor = ErrorColorSoft,
                        focusedLabelColor = SoftGreen,
                        errorLabelColor = ErrorColorSoft,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp) // Compact
                )

                OutlinedTextField(
                    value = quantityLimitString,
                    onValueChange = { value ->
                        val newQuantity = value.filter { it.isDigit() }
                        quantityLimitString = newQuantity
                        quantityLimitError = when {
                            newQuantity.isNotEmpty() && newQuantity.toIntOrNull() == null -> "Numbers only!" // Shorter error
                            newQuantity.isNotEmpty() && newQuantity.toIntOrNull()!! <= 0 -> "Must be > 0 or empty" // Shorter error
                            else -> null
                        }
                    },
                    label = { Text("Quantity? (Optional)", color = TextColorSecondary, fontSize = 13.sp) }, // Compact
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Empty for 1 per child", color = TextColorSecondary.copy(alpha=0.7f), fontSize = 12.sp) }, // Compact
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    isError = quantityLimitError != null,
                    supportingText = {
                        if (quantityLimitError != null) Text(quantityLimitError!!, color = ErrorColorSoft, fontSize = 11.sp) // Compact
                    },
                    shape = RoundedCornerShape(12.dp), // Compact
                    colors = OutlinedTextFieldDefaults.colors( // Restored full colors
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        errorBorderColor = ErrorColorSoft,
                        focusedLabelColor = SoftGreen,
                        errorLabelColor = ErrorColorSoft,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp) // Compact
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = TextColorSecondary, fontSize = 13.sp) }, // Compact
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 100.dp), // Compact: Reduced height
                    maxLines = 3, // Compact: Reduced max lines
                    shape = RoundedCornerShape(12.dp), // Compact
                    colors = OutlinedTextFieldDefaults.colors( // Restored full colors
                        focusedBorderColor = SoftGreen,
                        unfocusedBorderColor = TextColorSecondary.copy(alpha = 0.5f),
                        cursorColor = SoftGreen,
                        focusedLabelColor = SoftGreen,
                        focusedTextColor = TextColorPrimary,
                        unfocusedTextColor = TextColorPrimary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp) // Compact
                )

                // Requires Approval Checkbox and Info Message
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)) // Compact
                            .clickable(
                                enabled = !hasQuantityLimit,
                                onClick = {
                                    if (!hasQuantityLimit) requiresApproval = !requiresApproval
                                }
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp) // Compact
                    ) {
                        Checkbox(
                            checked = requiresApproval,
                            onCheckedChange = {
                                if (!hasQuantityLimit) requiresApproval = it
                            },
                            enabled = !hasQuantityLimit,
                            colors = CheckboxDefaults.colors( // Restored full colors
                                checkedColor = SoftGreen,
                                uncheckedColor = TextColorSecondary,
                                checkmarkColor = Color.White,
                                disabledCheckedColor = SoftGreen.copy(alpha = 0.6f),
                                disabledUncheckedColor = TextColorSecondary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(20.dp) // Compact
                        )
                        Spacer(Modifier.width(8.dp)) // Compact
                        Text(
                            "Accept/Reject?", // Shorter text
                            color = if (!hasQuantityLimit) TextColorPrimary else TextColorSecondary.copy(alpha = 0.7f),
                            fontSize = 13.sp, // Compact
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
                                .padding(start = 10.dp, top = 0.dp, end = 4.dp), // Compact
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp) // Compact
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Information",
                                tint = InfoColor,
                                modifier = Modifier.size(16.dp) // Compact
                            )
                            Text(
                                "Approval required if quantity is set.", // Shorter text
                                style = MaterialTheme.typography.labelSmall, // Compact
                                color = InfoColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp), // Compact
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), // Compact
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(10.dp), // Compact
                        colors = ButtonDefaults.textButtonColors(contentColor = TextColorSecondary), // Restored
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp) // Compact
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) // Compact
                    }

                    Button(
                        onClick = {
                            titleError = if (title.isBlank()) "Reward name can't be empty" else null
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
                                val updatedReward = rewardToEdit.copy(
                                    title = title.trim(),
                                    pointsRequired = points,
                                    description = description.trim().ifBlank { null },
                                    requiresApproval = requiresApproval,
                                    quantityLimit = if (quantityLimitString.isBlank()) null else quantityLimitValue
                                )
                                onConfirmEdit(updatedReward)
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(10.dp), // Compact
                        colors = ButtonDefaults.buttonColors( // Restored full colors
                            containerColor = SoftGreen,
                            contentColor = Color.White,
                            disabledContainerColor = TextColorSecondary.copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 2.dp), // Compact
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp) // Compact
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save Changes", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp) // Shorter text "Save"
                    }
                }
            }
        }
    }
}

