package com.veigar.questtracker.ui.screen.role

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.veigar.questtracker.NavRoutes
import com.veigar.questtracker.R
import com.veigar.questtracker.data.UserRepository
import com.veigar.questtracker.ui.component.EnhancedRoleCard
import com.veigar.questtracker.ui.theme.CoralBlueDark
import kotlinx.coroutines.launch

// Define more playful colors if desired, or use theme colors
val parentCardColor = Color(0xFF81D4FA) // Light Blue
val childCardColor = Color(0xFFA5D6A7)  // Light Green

@Composable
fun RoleSelectorScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    Box( // Use Box to overlay loading indicator if needed
        modifier = Modifier
            .fillMaxSize()
            .background(CoralBlueDark) // Your background
            .padding(16.dp), // Slightly reduced padding to give cards more space
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to QuestTracker", // More engaging
                style = MaterialTheme.typography.headlineMedium.copy( // Larger title
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text(
                text = "Are you a..?", // Updated question
                style = MaterialTheme.typography.titleMedium.copy( // Good size
                    color = Color.White.copy(alpha = 0.8f), // Your theme color
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(bottom = 20.dp) // More space after subtitle
            )

            // Enhanced Role Cards
            EnhancedRoleCard(
                title = "Parent",
                description = "Monitor tasks and location",
                imagePainter = painterResource(id = R.drawable.parent), // Ensure these drawables are appealing
                cardColor = parentCardColor,
                isLoading = loading,
                onClick = {
                    if (loading) return@EnhancedRoleCard
                    // Navigate to parent sub-role selection screen
                    navController.navigate(NavRoutes.ParentSubRole.route)
                }
            )

            EnhancedRoleCard(
                title = "Child",
                description = "Finish Quests. Gain XP!",
                imagePainter = painterResource(id = R.drawable.children), // Ensure these drawables are appealing
                cardColor = childCardColor,
                isLoading = loading,
                onClick = {
                    if (loading) return@EnhancedRoleCard
                    loading = true
                    scope.launch {
                        val result = UserRepository.saveUserRole("child")
                        if (result.isSuccess) {
                            navController.navigate(NavRoutes.ProfileSetup.route) {
                                popUpTo(NavRoutes.RoleSelector.route) { inclusive = true } // Avoid back to role selection
                            }
                        } else {
                            Toast.makeText(context, "Oops! Couldn't save role.", Toast.LENGTH_SHORT).show()
                        }
                        loading = false // Ensure loading is set to false in all paths
                    }
                }
            )
        }

        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }
    }
}