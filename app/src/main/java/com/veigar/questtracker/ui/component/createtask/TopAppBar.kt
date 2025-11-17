package com.veigar.questtracker.ui.component.createtask

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayText

//for create task app bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskTopBar(
    title: String = "Create New Quest",
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = ProfessionalGrayText
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ProfessionalGrayText
                )
            }
        },
        actions = {
            IconButton(onClick = onCreateClick) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Create Task",
                    tint = ProfessionalGrayText
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ProfessionalGray // Professional gray background
        )
    )
}
