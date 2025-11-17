package com.veigar.questtracker.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.veigar.questtracker.R
import com.veigar.questtracker.util.ImageManager
import kotlinx.coroutines.launch
import java.io.IOException
import androidx.core.graphics.createBitmap
import coil3.request.ImageRequest
import coil3.request.allowHardware
import java.io.ByteArrayOutputStream
@Composable
fun AvatarPicker(
    selectedAvatar: String, // Filename of the selected avatar (e.g., "char1.png")
    onAvatarSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    avatarDisplaySize: Dp = 90.dp, // Size of the main avatar display circle
    defaultIconColor: Color = MaterialTheme.colorScheme.primary // Color for the default placeholder
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally // Center the avatar display
    ) {
        // --- Clickable Selected Avatar Display ---
        Box(
            modifier = Modifier
                .size(avatarDisplaySize)
                .clip(CircleShape)
                .clickable { showDialog = true }
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(4.dp), // Padding inside the border for the image
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val imageModifier = Modifier
                .fillMaxSize() // Fill the Box defined above
                .clip(CircleShape)

            if (selectedAvatar.isNotBlank()) {
                if (isNetworkOrCustomPath(selectedAvatar)) {
                    val fullUrl = ImageManager.getFullUrl(selectedAvatar)
                    AsyncImage(
                        model = fullUrl,
                        contentDescription = "Selected Custom Avatar",
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.avatar_placeholder),
                        error = painterResource(id = R.drawable.avatar_placeholder),
                        onError = { error ->
                            Log.e("AvatarPicker", "Error loading image $fullUrl: ${error.result.throwable}")
                        }
                    )
                } else { // It's an asset
                    val assetBitmap = remember(selectedAvatar) {
                        loadBitmapFromAsset(context, selectedAvatar)
                    }
                    if (assetBitmap != null) {
                        Image(
                            bitmap = assetBitmap,
                            contentDescription = "Selected Asset Avatar: $selectedAvatar",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Optional: Text label below the avatar display
        Text(
            text = if (selectedAvatar.isNotBlank()) "Change Avatar" else "Choose Avatar",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { showDialog = true } // Make text clickable too
        )
    }

    // --- Avatar Selection Dialog ---
    if (showDialog) {
        AvatarSelectionDialog(
            currentSelectedAvatar = selectedAvatar,
            onDismissRequest = { showDialog = false },
            onAvatarChosen = {
                showDialog = false
                onAvatarSelected(it)
            }
        )
    }
}

@Composable
private fun AvatarSelectionDialog(
    currentSelectedAvatar: String,
    onDismissRequest: () -> Unit,
    onAvatarChosen: (String) -> Unit
) {
    val context = LocalContext.current
    // Consider making avatarList a parameter if it can change or for better testability
    val avatarList = remember { getAvatarListFromAssets(context) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Choose Your Avatar",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (avatarList.isEmpty()){
                    Text("No avatars found!", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 64.dp), // Adjust minSize as needed
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.heightIn(max = 350.dp) // Constrain height if many avatars
                    ) {
                        items(avatarList) { avatarFile ->
                            AvatarDialogItem(
                                avatarFile = avatarFile,
                                isSelected = avatarFile == currentSelectedAvatar,
                                onClick = { onAvatarChosen(avatarFile) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun AvatarDialogItem(
    avatarFile: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    itemSize: Dp = 64.dp // Size of each avatar item in the dialog
) {
    val context = LocalContext.current
    val avatarBitmap = remember(avatarFile) {
        loadBitmapFromAsset(context, "avatars/$avatarFile")
    }

    Box(
        modifier = Modifier
            .size(itemSize)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .padding(if (isSelected) 3.dp else 2.dp), // Inner padding for the image
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = avatarFile, // Accessibility: consider more descriptive names if possible
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder for individual items if bitmap fails to load (less likely for assets)
            Box(Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
        }
    }
}

@Composable
fun DisplayAvatar(
    fullAssetPath: String?, // e.g., "avatars/6.png" or null/blank
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    contentDescription: String = "User Avatar",
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter = rememberVectorPainter(Icons.Filled.AccountCircle),
    placeholderTint: Color = Color.White,
) {
    val context = LocalContext.current
    val displayModifier = modifier
        .size(size) // Apply default size if not overridden
        .clip(shape) // Clip to the specified shape

    Box(
        modifier = displayModifier
            .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
        contentAlignment = Alignment.Center
    ) {
        if (!fullAssetPath.isNullOrBlank()) {
            if (isNetworkOrCustomPath(fullAssetPath)) {
                val fullUrl = ImageManager.getFullUrl(fullAssetPath)
                Log.d("DisplayAvatar", "Full URL: $fullUrl")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullUrl)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(), // Fill the Box
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.avatar_placeholder),
                    error = painterResource(id = R.drawable.avatar_placeholder)
                )
            } else if(fullAssetPath.startsWith("http")){
                val fullUrl = fullAssetPath
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullUrl)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.avatar_placeholder),
                    error = painterResource(id = R.drawable.avatar_placeholder)
                )
            } else { // It's an asset path (e.g., "avatars/1.png" or just "1.png" if you adapt)
                // Ensure assetPath for loadBitmapFromAsset is correct
                val actualAssetPath = fullAssetPath
                val assetBitmap = remember(actualAssetPath) {
                    loadBitmapFromAsset(context, actualAssetPath)
                }
                if (assetBitmap != null) {
                    Image(
                        bitmap = assetBitmap,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}


// --- Helper functions (getAvatarListFromAssets, loadBitmapFromAsset) remain the same ---
// (Make sure they are accessible, e.g., in the same file or imported correctly)

fun getAvatarListFromAssets(context: Context): List<String> {
    return try {
        context.assets.list("avatars")?.toList()?.filterNotNull() ?: emptyList() // Added filterNotNull
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}

fun loadBitmapFromAsset(context: Context, assetPath: String): ImageBitmap? {
    return try {
        val inputStream = context.assets.open(assetPath)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        bitmap?.asImageBitmap()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ImageUploaderDialog(
    onDismissRequest: () -> Unit,
    userID: String,
    onUploadSuccess : (String) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // For preview
    val coroutineScope = rememberCoroutineScope()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri // Store URI for preview
                // The actual upload will happen when the user confirms
            }
            // Don't dismiss here yet, let user confirm or cancel after seeing preview
        }
    )

    Dialog(onDismissRequest = {
        if (!isLoading) onDismissRequest() // Prevent dismissal if loading
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(all = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Upload Custom Avatar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // --- Image Preview Area ---
                if (selectedImageUri != null) {
                    val bitmap = remember(selectedImageUri) {
                        try {
                            context.contentResolver.openInputStream(selectedImageUri!!)?.use {
                                BitmapFactory.decodeStream(it)?.asImageBitmap()
                            }
                        } catch (e: IOException) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        PreviewImageCapture(bitmap = bitmap.asAndroidBitmap()) {
                            previewBitmap = it
                        }
                    } else {
                        Text(
                            "Could not load preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- Loading Indicator ---
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        "Uploading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // --- Action Buttons when not loading ---
                    Text(
                        if (selectedImageUri == null) "Select an image from your gallery." else "Happy with this image?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (selectedImageUri == null) {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                // Start upload
                                val imageData = bitmapToByteArray(previewBitmap!!)
                                if (imageData != null) {
                                    isLoading = true
                                    ImageManager.uploadImage( // Assuming this is your manager's upload function
                                        imageData = imageData,
                                        category = "profile",
                                        filename = userID, // Using userID as filename
                                        onSuccess = { relativePath ->
                                            coroutineScope.launch { // Ensure UI updates are on main thread
                                                isLoading = false
                                                val cacheBustedIdentifier = "?v=${System.currentTimeMillis()}"
                                                onUploadSuccess(relativePath + cacheBustedIdentifier)
                                                onDismissRequest()
                                            }
                                        },
                                        onError = { error ->
                                            coroutineScope.launch {
                                                isLoading = false
                                            }
                                        }
                                    )
                                } else {
                                    coroutineScope.launch {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (selectedImageUri == null) Icons.Filled.PhotoLibrary else Icons.Filled.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(if (selectedImageUri == null) "Choose Image" else "Upload Image")
                    }
                }

                // --- Bottom Action: Cancel Button ---
                // Always enabled unless loading, to allow backing out.
                if (!isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (selectedImageUri != null) Arrangement.SpaceBetween else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedImageUri != null) {
                            // Option to re-select image if one is already previewed
                            TextButton(
                                onClick = {
                                    selectedImageUri = null // Clear preview to re-select
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isLoading
                            ) {
                                Text("Change Image")
                            }
                        }
                        TextButton(
                            onClick = onDismissRequest,
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

fun bitmapToByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(format, quality, stream)
    return stream.toByteArray()
}
fun isNetworkOrCustomPath(identifier: String): Boolean {
    return identifier.startsWith("profile/") // Specific check if your relative paths always start with "profile/"
}

@Composable
fun PreviewImageCapture(
    bitmap: Bitmap,
    onBitmapCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val imageBitmap = bitmap.asImageBitmap()

    AndroidView(factory = {
        ComposeView(context).apply {
            setContent {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White) // Optional background
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Processed image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Give time to layout before capturing
            post {
                val capturedBitmap = createBitmap(width, height)
                val canvas = Canvas(capturedBitmap)
                draw(canvas)
                onBitmapCaptured(capturedBitmap)
            }
        }
    })
}