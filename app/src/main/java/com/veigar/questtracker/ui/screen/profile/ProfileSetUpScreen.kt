package com.veigar.questtracker.ui.screen.profile

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.ui.component.AvatarPicker
import com.veigar.questtracker.ui.component.DatePickerDialog
import com.veigar.questtracker.ui.component.ImageUploaderDialog
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.util.IdEncoder
import kotlinx.coroutines.launch
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(navController: NavHostController) {

    var name by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf<LocalDate?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    // Date picker state
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var selectedAvatar by remember { mutableStateOf("") }
    var showImageUploader by remember { mutableStateOf(false) }
    
    // Store the user role, parent sub-role, and gender to avoid calling suspend function in date picker
    var userRole by remember { mutableStateOf("child") }
    var userParentSubRole by remember { mutableStateOf("") }
    var userGender by remember { mutableStateOf("") }
    
    // Load the user role, parent sub-role, and gender when component is first created
    LaunchedEffect(Unit) {
        val userProfile = UserRepository.getUserProfile()
        userRole = userProfile?.role ?: "child"
        userParentSubRole = userProfile?.parentSubRole ?: ""
        userGender = userProfile?.gender ?: ""
        
        // Auto-set gender for father and mother
        if (userRole == "parent") {
            when (userParentSubRole) {
                "father" -> userGender = "male"
                "mother" -> userGender = "female"
                // For guardian, keep existing gender or leave empty for user to select
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                CoralBlueDark
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Title
            Text(
                "Set Up Your Profile!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF8E1)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar
            AvatarPicker(
                selectedAvatar = selectedAvatar,
                onAvatarSelected = {
                    if (it == "0.png") {
                        showImageUploader = true
                    } else {
                        selectedAvatar = "avatars/$it"
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    disabledTextColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFFFD54F), // Yellow-ish
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                    disabledIndicatorColor = Color.Gray,
                    cursorColor = Color(0xFFFFD54F),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                    disabledLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Birthdate Picker
            OutlinedTextField(
                value = birthdate?.toString() ?: "",
                onValueChange = {},
                label = { Text("Birthdate") },
                enabled = false,
                trailingIcon = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = Color(0xFFFFF176))
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    disabledTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFFFFD54F), // Yellow-ish
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                    disabledIndicatorColor = Color.White,
                    cursorColor = Color(0xFFFFD54F),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                    disabledLabelColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender Selection - Only show for child role or guardian parent sub-role
            if (userRole == "child" || (userRole == "parent" && userParentSubRole == "guardian")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gender:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    
                    RadioButton(
                        selected = userGender == "male",
                        onClick = { userGender = "male" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFFD54F), // Yellow
                            unselectedColor = Color.White
                        )
                    )
                    Text(
                        text = "Male",
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                    )
                    
                    RadioButton(
                        selected = userGender == "female",
                        onClick = { userGender = "female" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFFD54F), // Yellow
                            unselectedColor = Color.White
                        )
                    )
                    Text(
                        text = "Female",
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue Button
            Button(
                enabled = !loading,
                onClick = {
                    if(selectedAvatar.isBlank()){
                        Toast.makeText(context, "Please select an avatar.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (name.isBlank()){
                        Toast.makeText(context, "Please enter a name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val minNameLength = 2
                    val maxNameLength = 10
                    val namePattern = Regex("^[a-zA-Z]{$minNameLength,$maxNameLength}$")

                    if (!name.matches(namePattern)) {
                        Toast.makeText(
                            context,
                            "Name must be $minNameLength-$maxNameLength letters only (a-z, A-Z).",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    if(birthdate.toString().isBlank()){
                        Toast.makeText(context, "Please select a birthdate.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Validate age based on role
                    val today = LocalDate.now()
                    val age = java.time.temporal.ChronoUnit.YEARS.between(birthdate, today)
                    val isParent = userRole.lowercase() == "parent"
                    
                    when {
                        age < 0 -> {
                            Toast.makeText(context, "Birthdate cannot be in the future", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        isParent && age < 18 -> {
                            Toast.makeText(context, "Parent must be at least 18 years old", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        !isParent && age > 18 -> {
                            Toast.makeText(context, "Child must be under 18 years old", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        !isParent && age < 3 -> {
                            Toast.makeText(context, "Child must be at least 3 years old", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        age > 120 -> {
                            Toast.makeText(context, "Please enter a valid birthdate", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                    }
                    
                    // Only require gender selection for child role or guardian parent sub-role
                    if ((userRole == "child" || (userRole == "parent" && userParentSubRole == "guardian")) && userGender.isBlank()) {
                        Toast.makeText(context, "Please select a gender.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    loading = true

                    scope.launch {
                        val user = UserModel(
                            name = name,
                            role = userRole,
                            parentSubRole = userParentSubRole,
                            gender = userGender,
                            avatarUrl = selectedAvatar,
                            xp = 0,
                            level = 1,
                            birthdate = birthdate.toString(),
                            uid = IdEncoder.encodeToBase64(FirebaseAuthRepository.currentUser()?.uid
                                ?: "null")
                        )
                        val result = UserRepository.saveUserProfile(user)
                        loading = false

                        if (result.isSuccess) {
                            val route = if (userRole == "parent") {
                                NavRoutes.ParentDashboard.route
                            } else {
                                NavRoutes.ChildDashboard.route
                            }
                            navController.navigate(route) {
                                popUpTo(0)
                            }
                        } else {
                            Toast.makeText(context, "Failed to save profile.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = yellow),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) { // <<<<<< If loading is true...
                    CircularProgressIndicator( // ...show the progress indicator
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else { // <<<<<< If loading is false...
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue", color = Color.White, fontWeight = FontWeight.Bold) // ...show the normal button content
                }
            }
        }

        DatePickerDialog(
            showDialog = showDatePickerDialog,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = { selectedDate ->
                // Validate birthdate
                val today = LocalDate.now()
                val age = java.time.temporal.ChronoUnit.YEARS.between(selectedDate, today)
                
                // Use the stored role to determine validation rules
                val isParent = userRole.lowercase() == "parent"
                
                when {
                    age < 0 -> {
                        Toast.makeText(context, "Birthdate cannot be in the future", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }
                    isParent && age < 18 -> {
                        Toast.makeText(context, "Parent must be at least 18 years old", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }
                    !isParent && age > 18 -> {
                        Toast.makeText(context, "Child must be under 18 years old", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }
                    !isParent && age < 3 -> {
                        Toast.makeText(context, "Child must be at least 3 years old", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }
                    age > 120 -> {
                        Toast.makeText(context, "Please enter a valid birthdate", Toast.LENGTH_LONG).show()
                        return@DatePickerDialog
                    }
                    else -> {
                        birthdate = selectedDate // Update the state in ProfileSetupScreen
                        Toast.makeText(context, "Birthdate selected successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                // showDatePickerDialog will be set to false by onDismissRequest in the dialog
            },
            initialSelectedDate = birthdate // Pass the current birthdate as initial selection
        )
        if (showImageUploader) {
            ImageUploaderDialog(
                onDismissRequest = { showImageUploader = false },
                userID = FirebaseAuthRepository.currentUser()?.uid ?: "",
                onUploadSuccess = { newAvatarUrl ->
                    selectedAvatar = newAvatarUrl
                }
            )
        }
    }
}