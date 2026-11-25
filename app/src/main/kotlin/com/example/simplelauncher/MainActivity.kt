package com.example.simplelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initLauncher(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps(this)
    }
}

@Composable
fun LauncherRoot(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(LauncherScreen.HOME) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = isDrawerOpen || currentScreen == LauncherScreen.SETTINGS) {
        when {
            isDrawerOpen -> isDrawerOpen = false
            currentScreen == LauncherScreen.SETTINGS -> currentScreen = LauncherScreen.HOME
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        when (currentScreen) {
            LauncherScreen.HOME -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        viewModel = viewModel,
                        onOpenDrawer = { isDrawerOpen = true },
                        onOpenSettings = { currentScreen = LauncherScreen.SETTINGS },
                        onSwipeLeft = { openBrowserApp(context) },
                        onSwipeRight = { openPhoneApp(context) },
                        onSwipeUp = { isDrawerOpen = true },
                        onSwipeDown = { expandNotificationPanel(context) }
                    )

                    AnimatedVisibility(
                        visible = isDrawerOpen,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        AppDrawerAndSearchSheet(
                            viewModel = viewModel,
                            onClose = { isDrawerOpen = false },
                            onOpenSettings = {
                                isDrawerOpen = false
                                currentScreen = LauncherScreen.SETTINGS
                            }
                        )
                    }
                }
            }

            LauncherScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = LauncherScreen.HOME }
                )
            }
        }
    }
}
