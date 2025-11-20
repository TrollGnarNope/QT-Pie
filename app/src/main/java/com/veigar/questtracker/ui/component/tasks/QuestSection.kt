package com.veigar.questtracker.ui.component.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.model.TaskModel
import com.veigar.questtracker.ui.theme.DailyQuestGradientEnd
import com.veigar.questtracker.ui.theme.DailyQuestGradientStart

@Composable
fun QuestSection(
    title: String,
    tasks: List<TaskModel>,
    emptyDescription: String = "No Quests Assigned",
    gradientStartColor: Color = DailyQuestGradientStart,
    gradientEndColor: Color = DailyQuestGradientEnd,
    modifier: Modifier = Modifier,
    onTaskClick: (TaskModel) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStartColor, gradientEndColor)
                ),
                shape = RoundedCornerShape(
                    12.dp
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
            style = TextStyle(
                color = Color.White,
                shadow = Shadow(
                    color = Color(0x80000000),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            )
        )

        AnimatedContent(
            targetState = tasks.isEmpty(),
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -it / 2 }) togetherWith
                        (fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { it / 2 })
            }, label = "QuestsSectionContentAnimation"
        ) { isEmpty ->
            if (isEmpty) {
                Text(
                    text = emptyDescription,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.8f),
                        shadow = Shadow(
                            color = Color(0x80000000),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            } else {
                AnimatedContent(
                    targetState = tasks,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 300)) + slideInVertically(animationSpec = tween(durationMillis = 300)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 300)) + slideOutVertically(animationSpec = tween(durationMillis = 300))
                    }, label = "QuestCardsListAnimation"
                ) { currentTaskList ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        currentTaskList.forEach { task ->
                            key(task.taskId) {
                                QuestCard(
                                    task,
                                    modifier = Modifier
                                        .clickable { onTaskClick(task) }
                                        .animateContentSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}