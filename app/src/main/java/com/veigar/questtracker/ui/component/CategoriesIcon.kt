package com.veigar.questtracker.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.error
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private suspend fun loadBitmapFromAssets(context: Context, assetPath: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: IOException) {
            println("IOException while loading asset: $assetPath - ${e.message}")
            null
        } catch (e: Exception) {
            println("Error loading asset: $assetPath - ${e.message}")
            null
        }
    }
}


/**
 * A composable function to load and display an image from the "assets/categories" folder.
 * Supports PNG, JPG, BMP, WEBP. For SVGs, you'd need a different loading mechanism (e.g., Coil with SVG support).
 *
 * @param imageNameWithExtension The full name of the image file including its extension
 *                              (e.g., "shopping.png") located in "assets/categories".
 * @param contentDescription A description for accessibility.
 * @param modifier Modifier for the Image composable.
 * @param size The desired size for the image.
 * @param contentScale Content scaling for the image.
 * @param defaultTint Optional tint to apply to the image if it's a monochrome icon.
 */
@Composable
fun AssetCategoryImage(
    imageNameWithExtension: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentScale: ContentScale = ContentScale.Fit,
    defaultTint: ColorFilter? = null // For tinting icons
) {
    val context = LocalContext.current
    var imageBitmap by remember(imageNameWithExtension) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageNameWithExtension) { mutableStateOf(true) }
    var hasError by remember(imageNameWithExtension) { mutableStateOf(false) }

    LaunchedEffect(imageNameWithExtension, context) {
        isLoading = true
        hasError = false
        imageBitmap = null

        if (imageNameWithExtension.isBlank()) {
            println("Warning: imageNameWithExtension is blank.")
            hasError = true
            isLoading = false
            return@LaunchedEffect
        }

        imageBitmap = loadBitmapFromAssets(context, "categories/$imageNameWithExtension")
        if (imageBitmap == null) {
            hasError = true
        }
        isLoading = false
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(size / 2),
                    strokeWidth = 2.dp
                )
            }
            hasError || imageBitmap == null -> {
                Icon(
                    imageVector = Icons.Filled.BrokenImage,
                    contentDescription = "Error loading $contentDescription",
                    modifier = Modifier.size(size * 0.8f), // Slightly smaller than container
                    tint = MaterialTheme.colorScheme.error
                )
            }
            imageBitmap != null -> {
                Image(
                    painter = BitmapPainter(imageBitmap!!),
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(), // Fill the Box
                    contentScale = contentScale,
                    colorFilter = defaultTint
                )
            }
        }
    }
}

fun listAssetFiles(
    context: Context,
    assetSubFolder: String,
    removeExtension: Boolean = true
): List<String> {
    return try {
        val assetManager = context.assets
        // assetManager.list() returns an array of all files and directories
        // in the given path. If the path is an empty string, it will list all
        // files and directories in the root of the assets folder.
        val files = assetManager.list(assetSubFolder)

        if (files == null) {
            println("Warning: No files found in assets/$assetSubFolder or folder does not exist.")
            return emptyList()
        }

        files.mapNotNull { fileName ->
            // Filter out potential sub-directories if any by checking for extensions
            // This is a basic check; more robust would be to check if assetManager.open() fails
            // for it being a directory, but for simple icon lists, this might suffice.
            if (!fileName.contains(".")) {
                // Potentially a sub-directory, or a file without extension.
                // For simplicity here, we assume category icons will have extensions.
                // If you have files without extensions you want to list, adjust this logic.
                // To be more robust, one could try to open each to see if it's a file.
                return@mapNotNull null
            }

            if (removeExtension) {
                fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
            } else {
                fileName
            }
        }
    } catch (e: IOException) {
        println("Error listing asset files from '$assetSubFolder': ${e.message}")
        emptyList()
    }
}