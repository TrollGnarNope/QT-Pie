package com.veigar.questtracker.ui.component.createtask

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.ui.theme.CoralBlueDarkest

@Composable
fun LabeledTextField(
    label: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFocusedColor: Color = CoralBlueDarkest,
    isUnfocusedColor: Color = CoralBlueDarkest.copy(alpha = 0.6f),
    maxLines: Int = 1,
    minLines: Int = maxLines,
    singleLine: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(if (isFocused) isFocusedColor else isUnfocusedColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if(!label.isBlank()){
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 15.sp),
                modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            cursorBrush = SolidColor(Color.White),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 17.sp,
                color = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 5.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }, // adjust vertical spacing
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun LabeledIntField(
    label: String = "",
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isFocusedColor: Color = CoralBlueDarkest,
    isUnfocusedColor: Color = CoralBlueDarkest.copy(alpha = 0.6f)
) {
    LabeledTextField(
        label = label,
        value = value.toString(),
        onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
        modifier = modifier,
        isFocusedColor = isFocusedColor,
        isUnfocusedColor = isUnfocusedColor,
        singleLine = true
    )
}