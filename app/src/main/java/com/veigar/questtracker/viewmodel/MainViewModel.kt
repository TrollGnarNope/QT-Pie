package com.veigar.questtracker.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow

object MainViewModel {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning
    fun startService() {
        _isServiceRunning.value = true
    }

    fun stopService() {
        _isServiceRunning.value = false
    }
}