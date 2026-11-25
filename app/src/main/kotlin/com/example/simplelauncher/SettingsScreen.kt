package com.example.simplelauncher

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchUrlInput by remember(settings.searchEngineUrl) {
        mutableStateOf(settings.searchEngineUrl)
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadContacts(context)
            Toast.makeText(context, "Kontakte geladen!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Berechtigung verweigert", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Launcher Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search & Contacts Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Suche & Kontakte", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kontakte-Suche aktivieren (Berechtigung)")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchUrlInput,
                            onValueChange = {
                                searchUrlInput = it
                                viewModel.updateSearchEngine(context, it)
                            },
                            label = { Text("Suchmaschinen-URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 1 to 7 Apps Selector
            item {
                Text(
                    text = "Startbildschirm-Apps (${settings.favoritePackages.size} / 7 ausgewählt)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF90CAF9)
                )
                Text(
                    text = "Wähle 1 bis maximal 7 Apps aus, die auf dem Startbildschirm angezeigt werden sollen.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            items(allApps, key = { it.packageName }) { app ->
                val isSelected = settings.favoritePackages.contains(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleFavoriteApp(context, app.packageName) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = app.label,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleFavoriteApp(context, app.packageName) },
                        enabled = isSelected || settings.favoritePackages.size < 7
                    )
                }
            }
        }
    }
}
