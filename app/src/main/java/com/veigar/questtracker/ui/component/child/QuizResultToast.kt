package com.veigar.questtracker.ui.component.child

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veigar.questtracker.R
import kotlinx.coroutines.delay

data class QuizOutcome(
    val quizTitle: String,
    val score: Int,
    val isRewarded: Boolean,
    val hpChange: Int,
    val pointsChange: Int,
    val isOverdue: Boolean = false
)

@Composable
fun QuizResultToast(
    showDialog: Boolean,
    outcome: QuizOutcome?,
    onDismissRequest: () -> Unit,
    autoDismissDelay: Long = 4000L
) {
    android.util.Log.d("QuizResultToast", "showDialog: $showDialog, outcome: $outcome")
    if (showDialog && outcome != null) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icon based on outcome
                    Image(
                        painter = painterResource(
                            id = when {
                                outcome.isRewarded -> R.drawable.approved
                                outcome.isOverdue -> R.drawable.decline
                                else -> R.drawable.decline
                            }
                        ),
                        contentDescription = if (outcome.isRewarded) "Quiz Rewarded" else "Quiz Punished",
                        modifier = Modifier.size(100.dp)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Title based on outcome
                    Text(
                        text = when {
                            outcome.isOverdue -> "Quiz Overdue!"
                            outcome.isRewarded -> "Quiz Complete!"
                            else -> "Quiz Penalty!"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            outcome.isRewarded -> Color(0xFFA5D6A7) // Light Green (same as QuestToast)
                            outcome.isOverdue -> Color(0xFFFF9800) // Orange
                            else -> Color(0xFFF44336) // Red
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Quiz title
                    Text(
                        text = outcome.quizTitle,
                        color = Color(0xFFA5D6A7), // Light Green (same as QuestToast)
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Score
                    Text(
                        text = "Score: ${outcome.score}%",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // HP and Points changes
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Points Change (HP changes moved to points)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (outcome.pointsChange > 0) Color(0x75FFFDE7) else Color(0x75FFEBEE)
                        ) {
                            Text(
                                text = if (outcome.pointsChange > 0) "⭐ +${outcome.pointsChange} Pts (based on points earned)" else "⭐ ${outcome.pointsChange} Pts",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = if (outcome.pointsChange > 0) Color(0xFFF9A825) else Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Overdue warning
                    if (outcome.isOverdue) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "This quiz was submitted after the deadline",
                            color = Color(0xFFFF9800),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            LaunchedEffect(Unit) {
                delay(autoDismissDelay)
                onDismissRequest()
            }
        }
    }
}

