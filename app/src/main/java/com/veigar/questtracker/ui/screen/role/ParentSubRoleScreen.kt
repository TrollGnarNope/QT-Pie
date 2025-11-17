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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import kotlinx.coroutines.launch

// Define colors for parent sub-roles
val fatherCardColor = Color(0xFF81D4FA) // Light Blue
val motherCardColor = Color(0xFFF8BBD9) // Light Pink
val guardianCardColor = Color(0xFFB39DDB) // Light Purple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSubRoleScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfessionalGrayDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ProfessionalGrayText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose the option that best describes you",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 20.dp, top = 16.dp)
                )

                // Father Card
                EnhancedRoleCard(
                    title = "Father",
                    description = "Biological or adoptive father",
                    imagePainter = painterResource(id = R.drawable.father), // You can create specific icons later
                    cardColor = fatherCardColor,
                    isLoading = loading,
                    onClick = {
                        if (loading) return@EnhancedRoleCard
                        loading = true
                        scope.launch {
                            val result = UserRepository.saveUserRole("parent", "father")
                            if (result.isSuccess) {
                                navController.navigate(NavRoutes.ProfileSetup.route) {
                                    popUpTo(NavRoutes.RoleSelector.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Couldn't save role.", Toast.LENGTH_SHORT).show()
                            }
                            loading = false
                        }
                    }
                )

                // Mother Card
                EnhancedRoleCard(
                    title = "Mother",
                    description = "Biological or adoptive mother",
                    imagePainter = painterResource(id = R.drawable.mom), // You can create specific icons later
                    cardColor = motherCardColor,
                    isLoading = loading,
                    onClick = {
                        if (loading) return@EnhancedRoleCard
                        loading = true
                        scope.launch {
                            val result = UserRepository.saveUserRole("parent", "mother")
                            if (result.isSuccess) {
                                navController.navigate(NavRoutes.ProfileSetup.route) {
                                    popUpTo(NavRoutes.RoleSelector.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Couldn't save role.", Toast.LENGTH_SHORT).show()
                            }
                            loading = false
                        }
                    }
                )

                // Guardian Card
                EnhancedRoleCard(
                    title = "Guardian",
                    description = "Legal guardian or caregiver",
                    imagePainter = painterResource(id = R.drawable.parent), // You can create specific icons later
                    cardColor = guardianCardColor,
                    isLoading = loading,
                    onClick = {
                        if (loading) return@EnhancedRoleCard
                        loading = true
                        scope.launch {
                            val result = UserRepository.saveUserRole("parent", "guardian")
                            if (result.isSuccess) {
                                navController.navigate(NavRoutes.ProfileSetup.route) {
                                    popUpTo(NavRoutes.RoleSelector.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Couldn't save role.", Toast.LENGTH_SHORT).show()
                            }
                            loading = false
                        }
                    }
                )
                
                // Add bottom spacing to prevent clipping
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
