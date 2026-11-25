package com.example.simplelauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "App konnte nicht geöffnet werden", Toast.LENGTH_SHORT).show()
    }
}

fun openPhoneApp(context: Context) {
    val intent = Intent(Intent.ACTION_DIAL)
    context.startActivity(intent)
}

fun dialNumber(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    context.startActivity(intent)
}

/**
 * Resolves the user's default browser package and launches its normal main activity
 * (just like tapping the browser icon), without forcing a new tab or URL.
 */
fun openBrowserApp(context: Context) {
    val pm = context.packageManager
    val probeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
    val resolveInfo = pm.resolveActivity(probeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    val browserPackage = resolveInfo?.activityInfo?.packageName

    val launchIntent = if (browserPackage != null && browserPackage != "android") {
        pm.getLaunchIntentForPackage(browserPackage)
    } else {
        null
    }

    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        try {
            val fallback = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_BROWSER
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (e: Exception) {
            Toast.makeText(context, "Kein Browser gefunden", Toast.LENGTH_SHORT).show()
        }
    }
}

fun searchWeb(context: Context, baseUrl: String, query: String) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$baseUrl$encoded"))
    context.startActivity(intent)
}

/**
 * Pulls down the Android System Notification Shade.
 */
@SuppressLint("WrongConstant", "PrivateApi")
fun expandNotificationPanel(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
        expandMethod.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
