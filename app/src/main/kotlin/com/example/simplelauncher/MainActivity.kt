package com.example.simplelauncher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

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
                        onSwipeUp = { isDrawerOpen = true }
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

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit
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
                                onSwipeLeft()
                            } else {
                                onSwipeRight()
                            }
                        } else if (absY > absX && totalDragY < -threshold) {
                            onSwipeUp()
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
        TopClock(onOpenSettings = onOpenSettings)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favoriteApps.isEmpty()) {
                Text(
                    text = "Tippe auf das Zahnrad oben rechts, um 1–7 Apps für den Startbildschirm auszuwählen.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onOpenSettings() }
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

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable { onOpenDrawer() },
            shape = RoundedCornerShape(27.dp),
            color = Color(0xFF252525),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Suche",
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Apps, Kontakte & Web durchsuchen...",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Drawer öffnen",
                    tint = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun TopClock(onOpenSettings: () -> Unit) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentTime,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Text(
                text = currentDate,
                fontSize = 16.sp,
                color = Color.LightGray
            )
        }

        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Einstellungen",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun AppRowItem(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            fontSize = 20.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var searchUrlInput by remember(settings.searchEngineUrl) { mutableStateOf(settings.searchEngineUrl) }

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

fun openBrowserApp(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com"))
    context.startActivity(intent)
}

fun searchWeb(context: Context, baseUrl: String, query: String) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$baseUrl$encoded"))
    context.startActivity(intent)
}
