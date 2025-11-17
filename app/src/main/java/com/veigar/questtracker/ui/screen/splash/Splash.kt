package com.veigar.questtracker.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.R
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController, viewModel: SplashViewModel = viewModel()) {
    val targetRoute = viewModel.navigationTarget.collectAsStateWithLifecycle()
    LaunchedEffect(targetRoute.value) {
        targetRoute.value?.let { route ->
            navController.navigate(route) {
                popUpTo(NavRoutes.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.icon_quest_tracker), // Replace with app logo or splash art
                contentDescription = "Splash Logo",
                modifier = Modifier.size(150.dp)
            )
        }
    }
}
