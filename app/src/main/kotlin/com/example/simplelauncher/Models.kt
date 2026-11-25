package com.example.simplelauncher

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap
)

data class ContactInfo(
    val name: String,
    val phoneNumber: String
)

data class LauncherSettings(
    val favoritePackages: List<String> = emptyList(),
    val searchEngineUrl: String = "https://duckduckgo.com/?q="
)

enum class LauncherScreen {
    HOME,
    SETTINGS
}
