package com.example.simplelauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.ContactsContract
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contacts: StateFlow<List<ContactInfo>> = _contacts.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    fun initLauncher(context: Context) {
        loadSettings(context)
        loadApps(context)
        loadContacts(context)
    }

    private fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val favString = prefs.getString("favorites", "") ?: ""
        val favList = if (favString.isBlank()) emptyList() else favString.split(",")

        _settings.value = LauncherSettings(
            favoritePackages = favList.take(7),
            searchEngineUrl = prefs.getString("search_url", "https://duckduckgo.com/?q=")
                ?: "https://duckduckgo.com/?q="
        )
    }

    private fun saveSettings(context: Context, newSettings: LauncherSettings) {
        _settings.value = newSettings
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("favorites", newSettings.favoritePackages.take(7).joinToString(","))
            .putString("search_url", newSettings.searchEngineUrl)
            .apply()
    }

    fun toggleFavoriteApp(context: Context, packageName: String) {
        val current = _settings.value.favoritePackages.toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else if (current.size < 7) {
            current.add(packageName)
        }
        saveSettings(context, _settings.value.copy(favoritePackages = current))
    }

    fun updateSearchEngine(context: Context, url: String) {
        saveSettings(context, _settings.value.copy(searchEngineUrl = url))
    }

    fun loadApps(context: Context) {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, 0)
                }

                resolveInfos
                    .filter { it.activityInfo.packageName != context.packageName }
                    .distinctBy { it.activityInfo.packageName }
                    .map { info ->
                        val bitmap = info.loadIcon(pm)
                            .toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888)
                            .asImageBitmap()
                        AppInfo(
                            label = info.loadLabel(pm).toString(),
                            packageName = info.activityInfo.packageName,
                            icon = bitmap
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            _allApps.value = apps

            if (_settings.value.favoritePackages.isEmpty() && apps.isNotEmpty()) {
                val defaultFavs = apps.take(5).map { it.packageName }
                saveSettings(context, _settings.value.copy(favoritePackages = defaultFavs))
            }
        }
    }

    fun loadContacts(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        viewModelScope.launch {
            val contactList = withContext(Dispatchers.IO) {
                val list = mutableListOf<ContactInfo>()
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )

                cursor?.use {
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (it.moveToNext()) {
                        val name = it.getString(nameIdx) ?: continue
                        val number = it.getString(numIdx) ?: continue
                        list.add(ContactInfo(name, number))
                    }
                }
                list.distinctBy { "${it.name}-${it.phoneNumber}" }
            }
            _contacts.value = contactList
        }
    }
}
