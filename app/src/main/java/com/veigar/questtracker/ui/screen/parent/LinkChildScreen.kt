package com.veigar.questtracker.ui.screen.parent

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link // Link Icon
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.ui.component.QRCodeScanner
import com.veigar.questtracker.ui.component.createtask.LabeledTextField
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.viewmodel.LinkChildViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

// Minimal state needed directly in the Composable for the input
// ViewModel would handle isLoading, feedback messages, actual linking logic

val LightTextColor = Color.White
val SubtleLightTextColor = Color(0xFFE4F5FC) // A lighter gray for less important text
val AccentColor = Color(0xFFFFECEC) // A light blue accent, adjust as needed

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LinkChildScreen(
    navController: NavController,
    viewModel: LinkChildViewModel = viewModel()
) {
    val childUserIdInput = viewModel.childUserIdInput
    val isLoading = viewModel.isLoading
    val feedbackMessage = viewModel.feedbackMessage
    val linkSuccessEvent = viewModel.linkSuccessEvent

    val context = LocalContext.current // For potential Toast messages or other context needs
    val snackbarHostState = remember { SnackbarHostState() }

    var showScanner by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Camera permission is required. Please enable it in settings.",
                actionLabel = "Open Settings",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            }
            showSnackbar = false
        }
    }

    LaunchedEffect(linkSuccessEvent) {
        if (linkSuccessEvent == true) {
            // Example: Show a Toast (optional, feedbackMessage already shows in UI)
            Toast.makeText(context, "Link successful!", android.widget.Toast.LENGTH_SHORT).show()

            // Navigate back or to a different screen on success
            navController.popBackStack() // Or navController.navigate("some_success_route")

            viewModel.consumeLinkSuccessEvent() // Reset the event in ViewModel
        }
    }

    BackHandler(enabled = true) {
        if (!isLoading && !showScanner){
            navController.popBackStack()
        } else if(showScanner){
            showScanner = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Link Child Account", // Changed title
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = ProfessionalGrayText // Professional gray text color
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalGray // Professional gray TopAppBar
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isLoading && !showScanner){
                            navController.popBackStack()
                        } else if(showScanner){
                            showScanner = false
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ProfessionalGrayText // Professional gray tint for icon
                        )
                    }
                }
            )
        },
        containerColor = ProfessionalGrayDark // Set professional gray background for the Scaffold
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. Link Icon
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = "Link Account Icon",
                tint = AccentColor, // Use a brighter accent color for the icon
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Headline and Sub-headline
            Text(
                text = "Link to a Child Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LightTextColor // Use light text color
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your child's user ID to link your accounts.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = SubtleLightTextColor, // Use a more subtle light color
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LabeledTextField(
                value = childUserIdInput,
                onValueChange = { viewModel.onChildUserIdChanged(it) },
                label = "Child's User ID",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (childUserIdInput.isNotBlank()) {
                        viewModel.onLinkChildClicked()
                    }
                },
                enabled = childUserIdInput.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = yellow,
                    contentColor = Color.White,
                    disabledContainerColor = SubtleLightTextColor.copy(alpha = 0.3f),
                    disabledContentColor = LightTextColor.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("LINK ACCOUNT", fontWeight = FontWeight.Bold)
                }
            }

            // Display feedback message
            feedbackMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    it,
                    color = if (it.contains("Successfully")) AccentColor else MaterialTheme.colorScheme.error, // Use Accent for success
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Below is a scan QR code card, clickable
            Text(
                "Alternatively:",
                style = MaterialTheme.typography.titleSmall,
                color = SubtleLightTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                onClick = {
                    if (!isLoading) {
                        showScanner = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Remove elevation on dark bg
                border = BorderStroke(1.dp, AccentColor.copy(alpha = 0.5f)), // Subtle border
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent, // Make card transparent, rely on border
                    // Or a very subtle dark shade: CoralBlueDark.copy(alpha=0.2f)
                    contentColor = LightTextColor // Text and icon color inside the card
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "Scan QR Code Icon",
                        tint = AccentColor, // Use accent for the icon
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Scan Child's QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextColor // Light text for the card
                    )
                }
            }
        }
        if (showScanner) {
            QRCodeScanner(
                modifier = Modifier.fillMaxSize(),
                onScanSuccess = { resultText ->
                    showScanner = false
                    viewModel.onChildUserIdChanged(resultText)
                    viewModel.onLinkChildClicked()
                },
                onScanFailure = { exception ->
                    showScanner = false // Hide scanner

                },
                onPermissionDenied = {
                    // Snackbar is already shown by QRCodeScanner.
                    // Here, just ensure the overlay is hidden.
                    showScanner = false
                    showSnackbar = true
                },
                onPermissionGranted = {
                    // Optional: Log or a brief, non-snackbar confirmation if needed.
                    // Toast.makeText(context, "Permission granted! Scanner active.", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}


