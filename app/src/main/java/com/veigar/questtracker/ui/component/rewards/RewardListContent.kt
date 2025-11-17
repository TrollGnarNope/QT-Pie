package com.veigar.questtracker.ui.component.rewards

import androidx.compose.animation.core.tween // For customizing animation duration/easing
import androidx.compose.foundation.ExperimentalFoundationApi // Required for animateItemPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items with key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.RewardModel
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewardListContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    rewards: List<RewardModel>,
    onDeleteClicked: (RewardModel) -> Unit,
    onEditClicked: (RewardModel) -> Unit,
    onHistoryClicked: (RewardModel) -> Unit,
    isParent: Boolean = false
) {
    val backgroundColor = if (isParent) ProfessionalGrayDark else CoralBlueDark
    val textColor = if (isParent) ProfessionalGrayText else Color.White
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoading && rewards.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = textColor)
        } else if (rewards.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No rewards created yet",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = textColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the '+' button to create your first reward",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = rewards,
                    key = { it.rewardId }
                ) { reward ->
                    RewardItem(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(durationMillis = 300),
                                fadeOutSpec = tween(durationMillis = 300),
                                placementSpec = tween(durationMillis = 500)
                            ),
                        reward = reward,
                        onDeleteClicked = onDeleteClicked,
                        onEditClicked = onEditClicked,
                        onHistoryClicked = onHistoryClicked,
                        isParent = isParent
                    )
                }
            }
        }
    }
}

