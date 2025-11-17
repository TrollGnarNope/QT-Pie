package com.veigar.questtracker.ui.component.child

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.rememberAsyncImagePainter
import com.veigar.questtracker.R
import com.veigar.questtracker.model.RepeatFrequency
import com.veigar.questtracker.ui.theme.CoralBlueLight
import com.veigar.questtracker.viewmodel.ChildDashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper function to create image Uri
fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val storageDir = context.cacheDir // Using cache directory
    val imageFile = File.createTempFile(
        imageFileName,
        ".jpg",
        storageDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // Ensure this matches your manifest
        imageFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitQuest(
    onDismiss: () -> Unit,
    onSubmit: (imagePath: String) -> Unit, // Callback for successful upload
    viewModel: ChildDashboardViewModel
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val selectedTask by viewModel.selectedTask.collectAsState()
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showOptions by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val parentProfile by viewModel.parentProfile.collectAsState()
    var nannyApprove by remember { mutableStateOf(false) }

    // Collect success event to call the callback and dismiss
    LaunchedEffect(Unit) {
        viewModel.uploadSuccessEvent.collectLatest { path ->
            path?.let {
                onSubmit(it)
            }
        }
    }

    // Reset ViewModel state when the composable is disposed (e.g., modal dismissed)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            viewModel.resetState()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = tempCameraUri
            viewModel.resetState()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isLoading) { // Prevent dismissal while loading
                onDismiss()
            }
        },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false } // Prevent swipe dismissal while loading
        ),
        containerColor = Color(0xFFE3F2FD),
        dragHandle = null
    ) {
        BackHandler(isLoading) {

        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(if (isLoading) 0.5f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedTask?.title ?: "",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF37474F),
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Upload proof for approval",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(250.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CoralBlueLight.copy(alpha = 0.3f))
                    .clickable(enabled = !isLoading) { // Disable click when loading
                        showOptions = !showOptions
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    val painter = rememberAsyncImagePainter(selectedImageUri)
                    Image(
                        painter = painter,
                        contentDescription = "Image Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.upload_image),
                        contentDescription = "Placeholder",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 300))
            ) {
                Spacer(modifier = Modifier.height(12.dp)) // Reduced spacer
                AnimatedVisibility(
                    visible = showOptions && !isLoading, // Hide options when loading
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300, delayMillis = 50)
                    ) + fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 50)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ImageSourceButton(
                            text = "Camera",
                            icon = Icons.Filled.CameraAlt,
                            onClick = {
                                val newUri = createImageUri(context)
                                tempCameraUri = newUri
                                cameraLauncher.launch(newUri)
                                showOptions = false
                            }
                        )
                        ImageSourceButton(
                            text = "Gallery",
                            icon = Icons.Filled.PhotoLibrary,
                            onClick = { galleryLauncher.launch("image/*"); showOptions = false }
                        )
                    }
                }
                if (showOptions && !isLoading) {
                    Spacer(modifier = Modifier.height(12.dp)) // Reduced spacer
                }
            }
            // Nanny approval checkbox (shown only if parent's nanny flag is true)
            if (parentProfile?.nanny == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = nannyApprove, onCheckedChange = { nannyApprove = it })
                    Text("Nanny Approve?", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDismiss,
                    enabled = !isLoading, // Disable when loading
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        selectedImageUri?.let { uri ->
                            val category : String = selectedTask.let { task ->
                                if(task?.repeat?.frequency == RepeatFrequency.WEEKLY){
                                    "proof/weekly"
                                } else {
                                    "proof/daily"
                                }
                            }
                            val currentSystemTime = System.currentTimeMillis()
                            val fileName = "proof_${selectedTask!!.taskId}_${currentSystemTime}"
                            viewModel.startImageUpload(context, uri, fileName = fileName, category = category, nannyApprove = nannyApprove)
                        }
                    },
                    enabled = selectedImageUri != null && !isLoading,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uploadError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (uploadError) "Retry" else "Submit")
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun ImageSourceButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White // Or MaterialTheme.colorScheme.onPrimary
            )
            Text(text, color = Color.White) // Or MaterialTheme.colorScheme.onPrimary
        }
    }
}
