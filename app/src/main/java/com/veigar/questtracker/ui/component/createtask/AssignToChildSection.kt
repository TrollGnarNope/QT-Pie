package com.veigar.questtracker.ui.component.createtask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.screen.parent.tab.ChildChip

@Composable
fun AssignToChildSection(
    modifier: Modifier = Modifier,
    children: List<UserModel>,
    selectedChildrenUids: Set<String>,      // Changed to Set<String> for multiple UIDs
    onChildSelectionChanged: (String) -> Unit, // Callback for a single child UID toggle
    enabled: Boolean = true
) {
    val sectionTextColor = Color.White // For titles/text on CoralBlueDark background

    Column(modifier = modifier) {
        Text(
            text = "Assign this Quest to:",
            style = MaterialTheme.typography.titleMedium,
            color = sectionTextColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (children.isEmpty()) {
            Text(
                text = "No children available to assign.",
                style = MaterialTheme.typography.bodyMedium,
                color = sectionTextColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(children, key = { child -> child.getDecodedUid() }) { childUser ->
                    ChildChip(
                        userModel = childUser,
                        isSelected = childUser.getDecodedUid() in selectedChildrenUids,
                        onClick = {
                            if (enabled) {
                                onChildSelectionChanged(childUser.getDecodedUid())
                            }
                        }
                    )
                }
            }
        }
    }
}
