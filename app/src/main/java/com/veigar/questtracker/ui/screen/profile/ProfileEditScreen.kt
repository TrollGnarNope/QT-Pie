package com.veigar.questtracker.ui.screen.profile


import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.ui.component.AvatarPicker
import com.veigar.questtracker.ui.component.DatePickerDialog
import com.veigar.questtracker.ui.component.ImageUploaderDialog
import com.veigar.questtracker.ui.component.QRCodeImage
import com.veigar.questtracker.ui.component.TwoColorPicker
import com.veigar.questtracker.ui.component.createtask.LabeledTextField
import com.veigar.questtracker.ui.component.createtask.ScheduleItemRow
import com.veigar.questtracker.ui.component.createtask.scheduleRowColors
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.util.debounced
import com.veigar.questtracker.viewmodel.ProfileEditViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O) // For potential LocalDate usage
@Composable
fun ProfileEditScreen(
    navController: NavController,
    viewModel: ProfileEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val initialUser by viewModel.initialUser.collectAsStateWithLifecycle()
    val userToEditFromViewModel by viewModel.user.collectAsStateWithLifecycle() // Renamed for clarity

    // States for editable fields, initialized and updated reactively
    var name by remember(userToEditFromViewModel?.name) {
        mutableStateOf(userToEditFromViewModel?.name ?: "")
    }
    var avatarUrl by remember(userToEditFromViewModel?.avatarUrl) {
        mutableStateOf(userToEditFromViewModel?.avatarUrl ?: "")
    }
    var birthdate by remember(userToEditFromViewModel?.birthdate) {
        mutableStateOf(userToEditFromViewModel?.birthdate ?: "")
    }
    var gender by remember(userToEditFromViewModel?.gender, userToEditFromViewModel?.parentSubRole) {
        val initialGender = userToEditFromViewModel?.gender ?: ""
        val parentSubRole = userToEditFromViewModel?.parentSubRole ?: ""
        
        // Auto-set gender for father and mother
        when (parentSubRole) {
            "father" -> mutableStateOf("male")
            "mother" -> mutableStateOf("female")
            else -> mutableStateOf(initialGender)
        }
    }

    var primaryColorHex by remember(userToEditFromViewModel?.firstColor) { // Assuming primaryColorHex exists on user model
        mutableStateOf(userToEditFromViewModel?.firstColor ?: "#FF6200EE") // Default primary
    }
    var secondaryColorHex by remember(userToEditFromViewModel?.secondColor) { // Assuming secondaryColorHex
        mutableStateOf(userToEditFromViewModel?.secondColor ?: "#FF03DAC5") // Default secondary
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    var showImageUploader by remember { mutableStateOf(false) }

    val isInitiallyLoadingUser by viewModel.isLoadingUser.collectAsStateWithLifecycle()

    // Loading state specifically for the save operation
    var isSavingProfile by remember { mutableStateOf(false) }
    
    // Determine if user is parent or child for theme selection - use remember to prevent transitions
    val isParent by remember(userToEditFromViewModel?.role) { 
        mutableStateOf(userToEditFromViewModel?.role?.lowercase() == "parent") 
    }
    var nanny by remember(userToEditFromViewModel?.nanny) {
        mutableStateOf(userToEditFromViewModel?.nanny == true)
    }
    val backgroundColor by remember(isParent) { 
        mutableStateOf(if (isParent) ProfessionalGrayDark else CoralBlueDark) 
    }
    val topBarColor by remember(isParent) { 
        mutableStateOf(if (isParent) ProfessionalGray else CoralBlueDark) 
    }
    val textColor by remember(isParent) { 
        mutableStateOf(if (isParent) ProfessionalGrayText else Color.White) 
    }

    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor
                ),
                title = {
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(debounced(
                        onClick = {
                            if (isSavingProfile) return@debounced
                            navController.popBackStack()
                        }
                    )) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(
                        debounced(
                            onClick = {
                                if (isSavingProfile) return@debounced
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                                    return@debounced
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
                                    return@debounced
                                }
                                
                                // Validate gender selection for child role or guardian parent sub-role
                                val userRole = userToEditFromViewModel?.role?.lowercase() ?: ""
                                val userParentSubRole = userToEditFromViewModel?.parentSubRole?.lowercase() ?: ""
                                
                                if ((userRole == "child" || (userRole == "parent" && userParentSubRole == "guardian")) && gender.isBlank()) {
                                    Toast.makeText(context, "Please select a gender.", Toast.LENGTH_SHORT).show()
                                    return@debounced
                                }
                                
                                // Validate age based on role
                                if (birthdate.isNotBlank()) {
                                    val today = LocalDate.now()
                                    val age = java.time.temporal.ChronoUnit.YEARS.between(LocalDate.parse(birthdate), today)
                                    val isParent = userRole == "parent"
                                    
                                    when {
                                        age < 0 -> {
                                            Toast.makeText(context, "Birthdate cannot be in the future", Toast.LENGTH_LONG).show()
                                            return@debounced
                                        }
                                        isParent && age < 18 -> {
                                            Toast.makeText(context, "Parent must be at least 18 years old", Toast.LENGTH_LONG).show()
                                            return@debounced
                                        }
                                        !isParent && age > 18 -> {
                                            Toast.makeText(context, "Child must be under 18 years old", Toast.LENGTH_LONG).show()
                                            return@debounced
                                        }
                                        !isParent && age < 3 -> {
                                            Toast.makeText(context, "Child must be at least 3 years old", Toast.LENGTH_LONG).show()
                                            return@debounced
                                        }
                                        age > 120 -> {
                                            Toast.makeText(context, "Please enter a valid birthdate", Toast.LENGTH_LONG).show()
                                            return@debounced
                                        }
                                    }
                                }
                                
                                userToEditFromViewModel?.let { currentUser ->
                                    isSavingProfile = true
                                    val updatedUser = currentUser.copy(
                                        name = name,
                                        avatarUrl = avatarUrl,
                                        birthdate = birthdate,
                                        gender = gender,
                                        firstColor = primaryColorHex,
                                        secondColor = secondaryColorHex,
                                        nanny = nanny
                                    )
                                    if(initialUser == updatedUser){
                                        Toast.makeText(context, "No changes detected", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                        return@debounced
                                    }

                                    viewModel.updateUserProfile(updatedUser) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        } else {
                                            isSavingProfile = false
                                            Toast.makeText(context, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Cannot save: User data not loaded.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ),
                        // Enable save only if not initially loading, not currently saving, and user data is present
                        enabled = !isInitiallyLoadingUser && !isSavingProfile && userToEditFromViewModel != null
                    ) {
                        if (isSavingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textColor)
                        } else {
                            Icon(Icons.Filled.Done, contentDescription = "Save Profile", tint = textColor)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (isInitiallyLoadingUser) {
                // Show full-screen loading indicator for initial user load
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading Profile...", color = MaterialTheme.colorScheme.onPrimary)
                }
            } else if (userToEditFromViewModel == null) {
                // Case: Loading finished, but user is still null (e.g., fetch error)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Failed to load profile. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    // Optional: Add a retry button here that calls a reload function in ViewModel
                    // Button(onClick = { viewModel.retryLoadUser() }) { Text("Retry") }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp), // Padding for the content within the scrollable column
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AvatarPicker(
                        selectedAvatar = avatarUrl,
                        onAvatarSelected = {
                            if (it == "0.png") {
                                showImageUploader = true
                            } else {
                                avatarUrl = "avatars/$it"
                            }
                        },
                        avatarDisplaySize = 100.dp,
                    )

                    LabeledTextField(
                        label = "Name",
                        value = name,
                        onValueChange = { name = it },
                        maxLines = 1
                    )

                    // Only show color picker for child users, not for parent users
                    if (userToEditFromViewModel?.role?.lowercase() != "parent") {
                        TwoColorPicker(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            initialPrimaryColorHex = primaryColorHex,
                            initialSecondaryColorHex = secondaryColorHex,
                            onPrimaryColorSelected = { newHex -> primaryColorHex = newHex },
                            onSecondaryColorSelected = { newHex -> secondaryColorHex = newHex }
                        )
                    }

                    // Parent-only: Nanny feature toggle
                    if (isParent) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = nanny, onCheckedChange = { nanny = it })
                            Text("Nanny", color = textColor, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    ScheduleItemRow(
                        label = "Birthdate:",
                        value = birthdate,
                        icon = Icons.Filled.Cake,
                        iconColor = Color.White,
                        textColor = Color.White,
                        backgroundColor = scheduleRowColors[1],
                        onClick = { showDatePickerDialog = true }
                    )
                    
                    // Gender Display/Selection - Always show gender field
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (gender.isNotEmpty()) {
                            // Show gender as a display row when it has a value (non-editable)
                            ScheduleItemRow(
                                label = "Gender",
                                value = gender.replaceFirstChar { it.uppercase() },
                                icon = Icons.Filled.Person,
                                iconColor = Color.White,
                                textColor = Color.White,
                                backgroundColor = scheduleRowColors[2],
                                onClick = {
                                    // Gender is not editable after being set
                                }
                            )
                        } else {
                            // Show gender selection UI when empty (first time setup)
                            Text(
                                text = "Please select your gender:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = gender == "male",
                                    onClick = { gender = "male" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFFD54F), // Yellow
                                        unselectedColor = Color.White
                                    )
                                )
                                Text(
                                    text = "Male",
                                    color = textColor,
                                    modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                                )
                                
                                RadioButton(
                                    selected = gender == "female",
                                    onClick = { gender = "female" },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFFD54F), // Yellow
                                        unselectedColor = Color.White
                                    )
                                )
                                Text(
                                    text = "Female",
                                    color = textColor,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                    
                    ScheduleItemRow(
                        label = "Role:",
                        value = userToEditFromViewModel?.role?.uppercase() ?: "",
                        icon = Icons.Filled.Person,
                        iconColor = Color.White,
                        textColor = Color.White,
                        backgroundColor = scheduleRowColors[0],
                        onClick = {}
                    )
                    ScheduleItemRow(
                        label = "User ID:",
                        value = userToEditFromViewModel?.uid ?: "",
                        icon = Icons.Filled.ContentCopy,
                        iconColor = Color.White,
                        textColor = Color.White,
                        backgroundColor = scheduleRowColors[0],
                        onClick = {
                            userToEditFromViewModel?.uid?.let {
                                clipboardManager.nativeClipboard.setPrimaryClip(ClipData.newPlainText("User ID", it))
                                Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    userToEditFromViewModel?.uid.let { userId ->
                        if (userId!!.isNotBlank()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally // Center content horizontally
                            ) {
                                Text(
                                    "QR Code:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                QRCodeImage(
                                    content = userId,
                                    size = 500,
                                    modifier = Modifier.padding(bottom = 16.dp).fillMaxSize(0.9f)
                                )
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
                            
                            // Check if user is parent or child for different age validations
                            val isParent = userToEditFromViewModel?.role?.lowercase() == "parent"
                            
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
                                    birthdate = selectedDate.toString() // Update the state in ProfileEditScreen
                                    Toast.makeText(context, "Birthdate updated successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                            // showDatePickerDialog will be set to false by onDismissRequest in the dialog
                        },
                        initialSelectedDate = try {
                            if (birthdate.isNotBlank()) LocalDate.parse(birthdate) else null
                        } catch (e: Exception) {
                            null // If parsing fails, start with no initial date
                        }
                    )
                    if (showImageUploader) {
                        ImageUploaderDialog(
                            onDismissRequest = { showImageUploader = false },
                            userID = userToEditFromViewModel?.getDecodedUid() ?: "",
                            onUploadSuccess = { newAvatarUrl ->
                                avatarUrl = newAvatarUrl
                            }
                        )
                    }
                }
            }
        }
    }
}
