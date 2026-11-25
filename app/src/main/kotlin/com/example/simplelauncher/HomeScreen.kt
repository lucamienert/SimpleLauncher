package com.example.simplelauncher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val favoriteApps = remember(allApps, settings.favoritePackages) {
        settings.favoritePackages.mapNotNull { pkg ->
            allApps.find { it.packageName == pkg }
        }
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        val absX = abs(totalDragX)
                        val absY = abs(totalDragY)
                        val threshold = 120f

                        if (absX > absY && absX > threshold) {
                            if (totalDragX < 0) {
                                onSwipeLeft()  // Left -> Browser
                            } else {
                                onSwipeRight() // Right -> Phone
                            }
                        } else if (absY > absX && absY > threshold) {
                            if (totalDragY < 0) {
                                onSwipeUp()    // Up -> Open Drawer & Search
                            } else {
                                onSwipeDown()  // Down -> Pull down Notification Shade
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Clock + Date + Settings button
        TopClock(onOpenSettings = onOpenSettings)

        // 2. Middle 1-7 Favorite Apps
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favoriteApps.isEmpty()) {
                Text(
                    text = "Tippe auf das Zahnrad oben rechts, um 1–7 Apps für den Startbildschirm auszuwählen.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onOpenSettings)
                )
            } else {
                favoriteApps.take(7).forEach { app ->
                    AppRowItem(
                        app = app,
                        onClick = { launchApp(context, app.packageName) }
                    )
                }
            }
        }

        // 3. Bottom Search Bar
        BottomSearchBar(onClick = onOpenDrawer)
    }
}
