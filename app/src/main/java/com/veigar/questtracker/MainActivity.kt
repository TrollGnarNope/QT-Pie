package com.veigar.questtracker

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.services.MainService
import com.veigar.questtracker.ui.theme.QuestTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    var notificationIdFromIntent: String? = null
    var notificationTypeFromIntent: String? = null

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        val activityViewModelStoreOwner = this as ViewModelStoreOwner
        handleIntent(intent)

        updateLastActiveTimestamp()

        setContent {
            QuestTrackerTheme {
                val navController = rememberNavController()
                MainNavHost(
                    navController = navController
                )
            }
        }
    }

    fun runService(){
        val startIntent = Intent(this, MainService::class.java).apply {
            action = MainService.ACTION_START_MONITORING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startIntent)
        } else {
            startService(startIntent)
        }
    }

    fun stopService(){
        val stopIntent = Intent(this, MainService::class.java)
        stopService(stopIntent)
    }

    override fun onResume() {
        super.onResume()
        updateLastActiveTimestamp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called with action: ${intent?.action}")
        setIntent(intent)
        handleIntent(intent)
        updateLastActiveTimestamp()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val notificationId = it.getStringExtra("notification_id_extra")
            val notificationType = it.getStringExtra("notification_type_extra")

            if (notificationId != null) {
                this.notificationIdFromIntent = notificationId
                this.notificationTypeFromIntent = notificationType
            }
        }
    }

    private fun updateLastActiveTimestamp() {
        activityScope.launch {
            try {
                UserRepository.updateLastActiveTimestamp()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error updating lastActiveTimeStamp", e)
            }
        }
    }
}

@Composable
fun SetSystemBarsColor(
    navBarColor: Color,
    statusBarColor: Color = navBarColor,
    darkIcons: Boolean = false
) {
    val context = LocalContext.current
    val view = LocalView.current

    SideEffect {
        val window = (context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navBarColor.toArgb()
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons.not()
    }
}