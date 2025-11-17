package com.veigar.questtracker.ui.screen.chats

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.veigar.questtracker.model.MessagesModel
import com.veigar.questtracker.ui.component.chats.ChatBox
import com.veigar.questtracker.ui.component.chats.ChatList
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.viewmodel.ChatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, viewModel: ChatsViewModel = viewModel()) {

    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var imageData by remember { mutableStateOf<ByteArray?>(null) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        navController.popBackStack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CoralBlueDark
                ),
                title = {
                    Text(
                        text = "Chats",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CoralBlueDark)
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            ChatList(
                messages = messages,
                currentUserId = user?.getDecodedUid() ?: "",
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            )
            ChatBox(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(
                            message = MessagesModel(
                                senderId = user?.getDecodedUid(),
                                senderAvatar = user?.avatarUrl,
                                message = messageText,
                                dateTime = System.currentTimeMillis().toString()
                            ),
                            imageData = imageData,
                            onUpload = {
                                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                        messageText = ""
                    }
                },
                modifier = Modifier,
                onAttachImage = {
                    imageData = it
                }
            )
        }
    }
}