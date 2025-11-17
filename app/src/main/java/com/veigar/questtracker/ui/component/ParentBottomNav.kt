package com.veigar.questtracker.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.CoralBlueLight
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import com.veigar.questtracker.ui.theme.ProfessionalGrayTextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun ParentBottomNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Home", Icons.Outlined.Home, "children"),
        NavItem("Quests", Icons.Outlined.CalendarMonth, "tasks"),
        NavItem("Zone", Icons.Outlined.LocationOn, "geofence"),
        NavItem("Alerts", Icons.Outlined.Notifications, "notifications")
    )

    val DpSaver = Saver<Dp, Float>(
        save = { it.value },
        restore = { Dp(it) }
    )

    val tabCenterX = remember { mutableStateMapOf<String, Float>() }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    var fabTargetOffsetDp by rememberSaveable(stateSaver = DpSaver) { mutableStateOf(0.dp) }
    var isFabVisible by rememberSaveable { mutableStateOf(false) }

    var hasLaunchedOnce by rememberSaveable { mutableStateOf(false) }

    val fabAnimatedOffsetX = remember { Animatable(fabTargetOffsetDp, Dp.VectorConverter) }

    val currentSelectedItem = remember(selectedTab) {
        items.find { it.route == selectedTab } ?: items.first()
    }

    LaunchedEffect(selectedTab, tabCenterX.toMap()) {
        val currentTargetX = tabCenterX[selectedTab]

        if (currentTargetX != null) {
            with(density) {
                val centerOfScreenPx = screenWidth.toPx() / 2
                val offsetPx = currentTargetX - centerOfScreenPx
                val newFabTargetOffsetDp = offsetPx.toDp()
                if (!hasLaunchedOnce) {
                    fabAnimatedOffsetX.snapTo(newFabTargetOffsetDp)
                    fabTargetOffsetDp = newFabTargetOffsetDp
                    delay(100)
                    isFabVisible = true
                    hasLaunchedOnce = true
                } else {
                    fabAnimatedOffsetX.animateTo(
                        newFabTargetOffsetDp,
                        animationSpec = tween(durationMillis = 300)
                    )
                    fabTargetOffsetDp = newFabTargetOffsetDp
                    isFabVisible = true
                }
                fabTargetOffsetDp = newFabTargetOffsetDp
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ProfessionalGrayDark)
                .height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { layoutCoordinates ->
                            val position = layoutCoordinates.positionInRoot()
                            val width = layoutCoordinates.size.width
                            val centerX = position.x + width / 2f
                            tabCenterX[item.route] = centerX
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isSelected,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 100)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300))
                    ) {
                        BottomNavItem(
                            item = item,
                            isSelected = false,
                            onClick = { onTabSelected(item.route) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isFabVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
        ) {
            Box(
                modifier = Modifier
                    .offset(x = fabAnimatedOffsetX.value, y = (-28).dp)
                    .size(50.dp)
                    .graphicsLayer {
                        rotationZ = 45f
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(10.dp)
                        clip = true
                    }
                    .background(Color(0xFFFCB827)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer { rotationZ = -45f }
                ) {
                    Icon(
                        imageVector = currentSelectedItem.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) ProfessionalGrayText else ProfessionalGrayTextSecondary

    Box(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1.0f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = item.label,
                fontSize = 13.sp,
                color = contentColor
            )
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector, val route: String)