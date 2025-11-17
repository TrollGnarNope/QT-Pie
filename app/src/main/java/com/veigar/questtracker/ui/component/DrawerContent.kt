package com.veigar.questtracker.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.veigar.questtracker.R
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayText

@Composable
fun DrawerContent(
    selectedRoute: String,
    onItemClick: (String) -> Unit,
    onDocumentationClick: (() -> Unit)? = null,
    isParent: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Scrollable Top Section ─────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()) // Enable scrolling
                .padding(bottom = 72.dp) // Leave space for Help Center section
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(if (isParent) ProfessionalGray else CoralBlueDark)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_quest_tracker),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 16.dp)
                        )
                        Text(
                            text = "QuestTracker",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isParent) ProfessionalGrayText else Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Drawer Items
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                DrawerItem(
                    label = "Profile",
                    icon = Icons.Outlined.AccountCircle,
                    isSelected = selectedRoute == "profile",
                    onClick = { onItemClick("profile") }
                )
                DrawerItem(
                    label = "Messages",
                    icon = Icons.Outlined.Chat,
                    isSelected = selectedRoute == "chat",
                    onClick = { onItemClick("chat") }
                )
                DrawerItem(
                    label = "Leaderboards",
                    icon = Icons.Outlined.EmojiEvents,
                    isSelected = selectedRoute == "leaderboards",
                    onClick = { onItemClick("leaderboards") }
                )
                DrawerItem(
                    label = "Quizzes",
                    icon = Icons.Outlined.Quiz,
                    isSelected = selectedRoute == "quizzes",
                    onClick = { onItemClick("quizzes") }
                )
                if (onDocumentationClick != null) {
                    DrawerItem(
                        label = "User Guide",
                        icon = Icons.Outlined.IntegrationInstructions,
                        isSelected = selectedRoute == "documentation",
                        onClick = { onDocumentationClick() }
                    )
                    DrawerItem(
                        label = "Help Center",
                        icon = Icons.AutoMirrored.Outlined.Help,
                        isSelected = selectedRoute == "help_center",
                        onClick = { onItemClick("help_center") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                DrawerItem(
                    label = "Logout",
                    icon = Icons.Outlined.Logout,
                    isSelected = false,
                    onClick = { onItemClick("logout") }
                )

            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}
