package com.veigar.questtracker.ui.screen.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.veigar.questtracker.data.FirebaseAuthRepository
import com.veigar.questtracker.data.VerificationThrottle
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun EmailVerificationDialog(
    onDismissVerified: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authRepo = FirebaseAuthRepository
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var isSending by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(0) }

    // Calculate initial countdown on first launch
    LaunchedEffect(Unit) {
        authRepo.reloadUser()
        if (authRepo.isEmailVerified()) {
            onDismissVerified()
            return@LaunchedEffect
        }
        
        // Check if we need to show countdown (if verification was recently sent)
        if (uid != null) {
            val now = System.currentTimeMillis()
            val lastResend = VerificationThrottle.getLastResendTime(context, uid)
            if (lastResend > 0) {
                val elapsed = now - lastResend
                val remaining = (60_000L - elapsed) / 1000L
                if (remaining > 0) {
                    countdownSeconds = remaining.toInt()
                }
            }
        }
    }

    // Countdown timer - decrements every second when countdownSeconds > 0
    LaunchedEffect(countdownSeconds) {
        if (countdownSeconds > 0) {
            delay(1000)
            // Re-check countdownSeconds in case it was reset or changed
            if (countdownSeconds > 0) {
                countdownSeconds--
            }
        }
    }

    AlertDialog(
        onDismissRequest = { /* Block dismiss */ },
        confirmButton = {},
        dismissButton = {},
        title = { Text("Verify your email") },
        text = {
            Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "We've sent a verification link to your email. Please verify to continue.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (!VerificationThrottle.canResend(context, uid, now)) {
                            val remaining = (60_000L - (now - VerificationThrottle.getLastResendTime(context, uid))) / 1000L
                            Toast.makeText(context, "Please wait ${remaining}s before resending.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSending = true
                        GlobalScope.launch(Dispatchers.Main) {
                            val result = authRepo.sendEmailVerification()
                            isSending = false
                            if (result.isSuccess) {
                                val sendTime = System.currentTimeMillis()
                                VerificationThrottle.recordResend(context, uid, sendTime)
                                countdownSeconds = 60 // Start 60 second countdown
                                Toast.makeText(context, "Verification email sent.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to send email: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSending && countdownSeconds == 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        when {
                            isSending -> "Sending..."
                            countdownSeconds > 0 -> "Resend verification (${countdownSeconds}s)"
                            else -> "Resend verification"
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        isChecking = true
                        GlobalScope.launch(Dispatchers.Main) {
                            authRepo.reloadUser()
                            isChecking = false
                            if (authRepo.isEmailVerified()) {
                                Toast.makeText(context, "Email verified!", Toast.LENGTH_SHORT).show()
                                onDismissVerified()
                            } else {
                                Toast.makeText(context, "Not verified yet.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White)
                ) {
                    Text(if (isChecking) "Checking..." else "I have verified")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text("Logout")
                }
            }
        }
    )
}


