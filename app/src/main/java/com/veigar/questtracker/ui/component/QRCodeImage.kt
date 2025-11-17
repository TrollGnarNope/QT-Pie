package com.veigar.questtracker.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap
import androidx.core.graphics.createBitmap

@Composable
fun QRCodeImage(
    modifier: Modifier = Modifier,
    content: String,
    size: Int = 500
) {
    var bitmap by remember { mutableStateOf(generateQRCode(content,size)) }
    Image(
        modifier = modifier,
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = "QR Code for: $content",
        contentScale = ContentScale.Fit
    )
}

private fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H // High error correction
        hints[EncodeHintType.MARGIN] = 1 // Less white space around QR code (0-4, 0 means no quiet zone)

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                // Set pixel color: black for true (QR code module), white for false (background)
                pixels[y * size + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        // Create a bitmap from the pixel array
        createBitmap(size, size).also {
            it.setPixels(pixels, 0, size, 0, 0, size, size)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}