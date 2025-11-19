package com.veigar.questtracker.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.R
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.data.VerificationThrottle
import com.veigar.questtracker.ui.theme.CoralBlue
import com.veigar.questtracker.ui.theme.TextSecondary
import com.veigar.questtracker.viewmodel.AuthState
import com.veigar.questtracker.viewmodel.AuthViewModel

@Composable
fun AuthScreen(navController: NavHostController, viewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    val state by viewModel.authState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Custom Icon / Logo area
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_quest_tracker),
                    contentDescription = "QuestTracker Icon",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isLoginMode) "QuestTracker" else "Join QuestTracker",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = if (isLoginMode) "Parental Control Made Simple" else "Create your account",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )

            // Forgot Password? (Only relevant for Login mode)
            if (isLoginMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = CoralBlue,
                        modifier = Modifier.clickable { viewModel.forgotPassword(email) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Action Button (Login or Register)
            Button(
                onClick = {
                    if (isLoginMode) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralBlue),
                enabled = state !is AuthState.Loading
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Login" else "Register",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle between Login/Register mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { isLoginMode = !isLoginMode }) {
                    Text(
                        text = if (isLoginMode) "Sign Up" else "Login",
                        color = CoralBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error and Message handling
            when (state) {
                is AuthState.Error -> {
                    Text(
                        text = (state as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is AuthState.AccountDisabled -> {
                    Text(
                        text = (state as AuthState.AccountDisabled).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                is AuthState.PasswordReset -> {
                    Text(
                        text = "Password reset email sent to ${(state as AuthState.PasswordReset).email}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> Unit
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Navigation Logic
    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            val user = UserRepository.getUserProfile()
            val userRole = user?.role

            // Verify throttle tracking for new users
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null && !currentUser.isEmailVerified) {
                val uid = currentUser.uid
                val now = System.currentTimeMillis()
                if (VerificationThrottle.getLastResendTime(context, uid) == 0L) {
                    VerificationThrottle.recordResend(context, uid, now)
                }
            }

            if(userRole == null){
                navController.navigate(NavRoutes.RoleSelector.route) {
                    popUpTo(0)
                }
            } else {
                if(userRole == "parent"){
                    navController.navigate(NavRoutes.ParentDashboard.route) {
                        popUpTo(0)
                    }
                } else {
                    navController.navigate(NavRoutes.ChildDashboard.route) {
                        popUpTo(0)
                    }
                }
            }
        }
    }
}