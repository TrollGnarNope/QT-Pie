package com.veigar.questtracker.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.veigar.questtracker.ui.component.pickRandomColor
import com.veigar.questtracker.util.IdEncoder
import kotlinx.serialization.Serializable

@Serializable
data class UserModel(
    val name: String = "",
    val role: String = "",
    val parentSubRole: String = "", // father, mother, guardian
    val gender: String = "", // male, female
    val avatarUrl: String = "",
    val xp: Int = 0,
    val level: Int = 1,
    val birthdate: String = "",
    val uid: String = "",
    val linkedChildIds: List<String> = emptyList(),
    val parentLinkedId: String? = null,
    val lastActiveTimeStamp: String = "",
    val lastDailyResetTimeStamp: String = "",

    val firstColor: String = pickRandomColor().toHexString(),
    val secondColor: String = pickRandomColor().toHexString(),

    // Parent features
    val nanny: Boolean = false,

    val hp: Int = 100,
    val mp: Int = 0,
    val atk: Int = 0,
    val def: Int = 0,
    val sta: Int = 0,
    val pts: Int = 0,
    val gems: Int = 0
) {
    fun getDecodedUid(): String {
        return IdEncoder.decodeFromBase64(uid)
    }
}

fun Color.toHexString() : String {
    return String.format("#%08X", this.toArgb())
}

fun String.toComposeColor(): Color {
    val colorString = if (this.startsWith("#")) this.substring(1) else this
    // Ensure we handle both 6-digit (RGB) and 8-digit (ARGB) hex strings.
    // If RGB, prepend FF for full alpha.
    val correctedColorString = when (colorString.length) {
        6 -> "FF$colorString" // Assume full alpha for RGB
        8 -> colorString       // ARGB is already fine
        else -> throw IllegalArgumentException("Invalid hex color string: $this. Must be 6 or 8 digits after #.")
    }
    return Color("#$correctedColorString".toColorInt())
}
