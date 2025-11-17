package com.veigar.questtracker.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.maps.model.BitmapDescriptor
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import java.io.InputStream
import androidx.core.graphics.createBitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory


object ImageManager {
    private val client = OkHttpClient()
    private const val BASE_URL = "https://guidegrowth.pythonanywhere.com"

    // Original function: Accepts an image File
    fun uploadImage(
        imageFile: File,
        category: String,
        filename: String,
        onSuccess: (relativePath: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val url = "$BASE_URL/upload_raw/$category?name=$filename"

        // Create RequestBody from File
        val body = imageFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        executeUpload(request, onSuccess, onError)
    }

    // NEW OVERLOADED FUNCTION: Accepts an image ByteArray (e.g., from a Bitmap)
    fun uploadImage(
        imageData: ByteArray,
        category: String,
        filename: String,
        onSuccess: (relativePath: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val url = "$BASE_URL/upload_raw/$category?name=$filename"

        // Create RequestBody from ByteArray
        val body =
            imageData.toRequestBody(
                "application/octet-stream".toMediaTypeOrNull(),
                0,
                imageData.size
            )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        executeUpload(request, onSuccess, onError)
    }

    // Helper function to handle the common HTTP request execution and response parsing
    private fun executeUpload(
        request: Request,
        onSuccess: (relativePath: String) -> Unit,
        onError: (error: String) -> Unit
    ) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Upload failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Server error: ${response.code} - ${response.message}")
                    return
                }

                val json = response.body.string()
                val urlRegex = Regex("\"url\"\\s*:\\s*\"(.*?)\"")
                val match = urlRegex.find(json)

                val uploadedUrl = match?.groups?.get(1)?.value
                if (uploadedUrl != null) {
                    val uploadsIndex = uploadedUrl.indexOf("/uploads/")
                    if (uploadsIndex >= 0) {
                        val relativePath = uploadedUrl.substring(uploadsIndex + "/uploads/".length)
                        onSuccess(relativePath)
                    } else {
                        // Fallback in case /uploads/ is not in the URL, return full URL
                        onSuccess(uploadedUrl)
                    }
                } else {
                    onError("URL not found in response")
                }
            }
        })
    }

    fun getFullUrl(relativePath: String): String {
        // Accepts:
        // - Full URLs (http/https) → return as-is
        // - Paths starting with "/uploads/" → strip prefix, then prefix BASE_URL/uploads
        // - Bare relative paths like "proof/daily/file.jpg" → prefix BASE_URL/uploads
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath
        }
        val cleaned = if (relativePath.startsWith("/uploads/")) {
            relativePath.removePrefix("/uploads/")
        } else if (relativePath.startsWith("uploads/")) {
            relativePath.removePrefix("uploads/")
        } else {
            relativePath
        }
        return "$BASE_URL/uploads/$cleaned"
    }

    fun downloadImage(
        relativePath: String,
        cacheDir: File,
        onSuccess: (file: File) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val url = getFullUrl(relativePath)
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Download failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Server error: ${response.code} - ${response.message}")
                    return
                }

                response.body.byteStream().use { inputStream ->
                    // Use the last path segment of the resolved URL as file name
                    val fileName = url.substringAfterLast('/')
                    val file = File(cacheDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    onSuccess(file)
                }
            }
        })
    }


    fun deleteImage(
        relativePath: String,
        onSuccess: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        val url = getFullUrl(relativePath)
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Delete failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Server error: ${response.code} - ${response.message}")
                }
            }
        })
    }

    private const val MAX_IMAGE_SIZE_BYTES = 500 * 1024 // 300KB

    fun compressImage(inputStream: InputStream, quality: Int): ByteArray {
        // Read the input stream fully so we can decode the bitmap
        val originalBytes = inputStream.readBytes()

        // Decode bitmap from the bytes
        val decodedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(originalBytes))
            ?: return originalBytes // Fallback: if decode fails, upload original bytes

        var currentQuality = quality
        val outputStream = ByteArrayOutputStream()
        decodedBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)

        // Iteratively reduce quality if the image is too large
        while (outputStream.toByteArray().size > MAX_IMAGE_SIZE_BYTES && currentQuality > 10) {
            currentQuality -= 10
            if (currentQuality < 10) currentQuality = 10

            outputStream.reset()
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
        }

        val compressedBytes = outputStream.toByteArray()
        outputStream.close()
        decodedBitmap.recycle()
        return compressedBytes
    }

    fun createMarkerBitmapDescriptor(
        view: View,
        content: @Composable () -> Unit
    ): BitmapDescriptor {
        val composeView = ComposeView(view.context).apply {
            setContent(content)
        }
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        val bitmap = createBitmap(composeView.measuredWidth, composeView.measuredHeight)
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}