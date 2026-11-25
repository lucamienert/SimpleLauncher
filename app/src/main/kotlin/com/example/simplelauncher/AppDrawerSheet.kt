package com.example.simplelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppDrawerAndSearchSheet(
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val allApps by viewModel.allApps.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val dismissDrawer = {
        keyboardController?.hide()
        onClose()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val filteredApps = remember(allApps, query) {
        if (query.isBlank()) allApps
        else allApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val filteredContacts = remember(contacts, query) {
        if (query.isBlank()) emptyList()
        else contacts.filter {
            it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
        }.take(5)
    }

    // Detects swipe-down when list is at the top to close the drawer
    var dragDownAccumulator by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    dragDownAccumulator = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0) {
                    dragDownAccumulator += available.y
                    if (dragDownAccumulator > 100f) {
                        dismissDrawer()
                        dragDownAccumulator = 0f
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dragDownAccumulator = 0f
                return super.onPostFling(consumed, available)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .nestedScroll(nestedScrollConnection)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Top Drag Handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 15f) {
                            dismissDrawer()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("Apps, Kontakte oder Web...") },
            leadingIcon = {
                IconButton(onClick = dismissDrawer) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Schließen")
                }
            },
            trailingIcon = {
                Row {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Leeren")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (filteredApps.size == 1) {
                        launchApp(context, filteredApps.first().packageName)
                        dismissDrawer()
                    } else if (query.isNotBlank()) {
                        searchWeb(context, settings.searchEngineUrl, query)
                        dismissDrawer()
                    }
                }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Web Search Option
            if (query.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchWeb(context, settings.searchEngineUrl, query)
                                dismissDrawer()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Websuche",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Im Web suchen nach „$query“",
                            fontSize = 17.sp,
                            color = Color(0xFF64B5F6)
                        )
                    }
                }
            }

            // Contacts
            if (filteredContacts.isNotEmpty()) {
                item {
                    Text(
                        text = "KONTAKTE",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(filteredContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dialNumber(context, contact.phoneNumber)
                                dismissDrawer()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = contact.name,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = contact.name, fontSize = 18.sp, color = Color.White)
                            Text(text = contact.phoneNumber, fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // All Apps
            item {
                Text(
                    text = "APPS (${filteredApps.size})",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(filteredApps, key = { it.packageName }) { app ->
                AppRowItem(
                    app = app,
                    onClick = {
                        launchApp(context, app.packageName)
                        dismissDrawer()
                    }
                )
            }
        }
    }
}
