package com.veigar.questtracker.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veigar.questtracker.data.ChatsRepository
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.MessagesModel
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.util.ImageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatsViewModel: ViewModel() {
    private val _user = MutableStateFlow<UserModel?>(null)
    val user: StateFlow<UserModel?> = _user.asStateFlow()

    private var _parentId: String? = "" //used for the chat room
    private val  _messages = MutableStateFlow<List<MessagesModel>>(emptyList())
    val messages: StateFlow<List<MessagesModel>> = _messages.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser(){
        UserRepository.observeUserProfile()
            .onEach { userModel ->
                _user.value = userModel
                if(userModel != null){
                    _parentId = if(_user.value?.role == "parent"){
                        _user.value!!.getDecodedUid()
                    } else {
                        _user.value!!.parentLinkedId!!
                    }
                    loadMessages()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadMessages(){
        viewModelScope.launch(Dispatchers.IO) {
            ChatsRepository.getChatMessagesFlow(_parentId!!)
                .onEach { messages ->
                    _messages.value = messages
                }
                .launchIn(this)
        }
    }

    fun sendMessage(message: MessagesModel, imageData: ByteArray? = null, onUpload: () -> Unit, onError: (error: String) -> Unit){
        viewModelScope.launch {
            var messageToSend = message
            if(imageData != null){
                onUpload()
                ImageManager.uploadImage(
                    imageData = imageData,
                    category = "chatImages",
                    filename = "${message.senderId}_${System.currentTimeMillis()}",
                    onSuccess = {
                        messageToSend = message.copy(img = ImageManager.getFullUrl(it))
                        ChatsRepository.sendMessage(_parentId!!, messageToSend) { success, messageId ->
                            if (success) {
                                // Message sent successfully
                            } else {
                                onError("Error sending message")
                            }
                        }
                    },
                    onError = {
                        onError("Error uploading image")
                    },
                )
                return@launch
            }
            ChatsRepository.sendMessage(_parentId!!, messageToSend) { success, messageId ->
                if (success) {
                    // Message sent successfully
                } else {
                    onError("Error sending message")
                }
            }
        }
    }
}