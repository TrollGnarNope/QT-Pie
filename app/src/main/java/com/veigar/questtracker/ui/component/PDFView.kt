package com.veigar.questtracker.ui.component

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap

@Composable
fun PDFView(assetPath: String, modifier: Modifier = Modifier, lastPageContent: @Composable () -> Unit = {}){
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var parcelFileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val bitmaps by produceState<List<Bitmap>>(initialValue = emptyList(), assetPath) {
        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "temp_pdf.pdf")
                context.assets.open(assetPath).use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                parcelFileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
                val newBitmaps = mutableListOf<Bitmap>()
                pdfRenderer?.pageCount?.let { pageCount ->
                    for (i in 0 until pageCount) {
                        if (!isActive) break // Check if coroutine is still active
                        val page = pdfRenderer!!.openPage(i)
                        val bitmap = createBitmap(page.width, page.height)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        newBitmaps.add(bitmap)
                        page.close()
                    }
                }
                value = newBitmaps
            } catch (e: Exception) {
                // Handle exceptions, e.g., log them or show an error message
                e.printStackTrace()
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(bitmaps.size) { index ->
                Image(
                    bitmap = bitmaps[index].asImageBitmap(),
                    contentDescription = "PDF Page ${index + 1}", modifier = Modifier.fillMaxWidth())
            }
            if (bitmaps.isNotEmpty()){
                item { lastPageContent() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                parcelFileDescriptor?.close()
                File(context.cacheDir, "temp_pdf.pdf").delete()
            } catch (_: Exception) {

            }
        }
    }
}