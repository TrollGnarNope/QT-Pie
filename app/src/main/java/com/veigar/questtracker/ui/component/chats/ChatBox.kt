package com.veigar.questtracker.ui.component.chats

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.veigar.questtracker.ui.component.createtask.LabeledTextField
import com.veigar.questtracker.ui.theme.CoralBlueDarkest
import com.veigar.questtracker.util.ImageManager
import java.io.InputStream

@Composable
fun ChatBox(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachImage: (ByteArray?) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageBytes: ByteArray? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                imageBytes = inputStream?.let { it1 -> ImageManager.compressImage(it1, 80) }
                inputStream?.close()
                Toast.makeText(context, "Image selected", Toast.LENGTH_SHORT).show()
                onAttachImage(imageBytes)
            }
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        ) {
            Icon(
                imageVector = if (imageBytes != null) Icons.Filled.FilePresent else Icons.Filled.AttachFile,
                contentDescription = "Attach Image",
                tint = Color.White
            )
        }

        LabeledTextField(
            label = "",
            value = messageText,
            onValueChange = {
                onMessageChange(it)
            },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            maxLines = 1,
        )
        IconButton(
            onClick = {
                onSendMessage()
                imageBytes = null
                onAttachImage(null)
            },
            enabled = messageText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send Message",
                tint = if (messageText.isNotBlank()) Color.White else CoralBlueDarkest
            )
        }
    }
}