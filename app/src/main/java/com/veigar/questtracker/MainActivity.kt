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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.services.MainService
import com.veigar.questtracker.ui.theme.QuestTrackerTheme
import com.veigar.questtracker.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.jvm.java

class MainActivity : ComponentActivity() {

    var notificationIdFromIntent: String? = null
    var notificationTypeFromIntent: String? = null
    
    // Coroutine scope for background operations
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        val activityViewModelStoreOwner = this as ViewModelStoreOwner
        handleIntent(intent)
        
        // Update last active timestamp when app is created
        updateLastActiveTimestamp()
        
        setContent {
            QuestTrackerTheme {
                val navController = rememberNavController()
                MainNavHost(
                    navController = navController,
                    startDestination = NavRoutes.Splash.route,
                    activityViewModelStoreOwner = activityViewModelStoreOwner,
                    this
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
        // Update last active timestamp when app comes to foreground
        updateLastActiveTimestamp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called with action: ${intent?.action}")
        setIntent(intent)
        handleIntent(intent)
        // Update timestamp when app is brought to foreground via intent
        updateLastActiveTimestamp()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            // Retrieve extras
            val notificationId = it.getStringExtra("notification_id_extra")
            val notificationType = it.getStringExtra("notification_type_extra")

            if (notificationId != null) {
                Log.i("MainActivity", "Received Notification ID: $notificationId")
                Log.i("MainActivity", "Received Notification Type: $notificationType")

                // Store them or pass them to where they are needed (e.g., ViewModel, Compose)
                this.notificationIdFromIntent = notificationId
                this.notificationTypeFromIntent = notificationType
            } else {
                Log.d("MainActivity", "No 'notification_id_extra' found in intent.")
            }
            // }

            // Make sure to clear the extras if they are meant to be processed only once,
            // especially if the activity might be re-launched with the same intent (e.g. from recents)
            // One way is to remove them after processing:
            // it.removeExtra("notification_id_extra")
            // it.removeExtra("notification_type_extra")
            // This is more critical if you don't use SINGLE_TOP or singleTask and always
            // want fresh processing. With SINGLE_TOP/singleTask and onNewIntent, you get the new intent.
        }
    }

    /**
     * Updates the lastActiveTimeStamp for the current user
     */
    private fun updateLastActiveTimestamp() {
        activityScope.launch {
            try {
                val result = UserRepository.updateLastActiveTimestamp()
                if (result.isSuccess) {
                    Log.d("MainActivity", "Successfully updated lastActiveTimeStamp")
                } else {
                    Log.w("MainActivity", "Failed to update lastActiveTimeStamp: ${result.exceptionOrNull()?.message}")
                }
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

        // Apply nav + status bar colors
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navBarColor.toArgb()

        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons.not() // false = white icons
    }
}