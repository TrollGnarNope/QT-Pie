package com.veigar.questtracker.util

import android.util.Base64
import java.nio.charset.StandardCharsets

object IdEncoder { // Using an object for singleton utility functions

    /**
     * Encodes a Firebase UID string to a Base64 string.
     * Uses Base64.NO_WRAP to prevent newlines and UTF-8 encoding.
     */
    fun encodeToBase64(text: String): String {
        return Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64 string back to the original Firebase UID string.
     * Uses Base64.NO_WRAP and UTF-8 decoding.
     */
    fun decodeFromBase64(base64Text: String): String {
        return String(Base64.decode(base64Text, Base64.NO_WRAP), StandardCharsets.UTF_8)
    }
}