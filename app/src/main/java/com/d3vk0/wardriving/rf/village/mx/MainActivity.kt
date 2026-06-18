package com.d3vk0.wardriving.rf.village.mx

import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPin
import com.d3vk0.wardriving.rf.village.mx.core.domain.MapPinType
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionSettings
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity
import com.d3vk0.wardriving.rf.village.mx.ui.theme.WardrivingTheme
import dagger.hilt.android.AndroidEntryPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DisposableEffect(uiState.settings.keepScreenAwake) {
                if (uiState.settings.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            WardrivingTheme {
                WardrivingApp(uiState, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WardrivingApp(uiState: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var route by remember { mutableStateOf(if (uiState.authenticated) "dashboard" else "login") }
    var selectedSession by remember { mutableStateOf<WardrivingSessionEntity?>(null) }
    var permissionRequestAttempted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.all { it } && WardrivingPermissions.hasRequiredRuntimePermissions(context)
        route = if (granted) "dashboard" else "settings"
    }
    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(if (uiState.lastExportPath?.endsWith(".zip") == true) "application/zip" else "text/csv"),
    ) { uri ->
        if (uri != null) viewModel.saveLastExportAs(uri)
    }
    val requestMissingPermissions = {
        val missing = WardrivingPermissions.missingRuntimePermissions(context)
        if (missing.isEmpty()) {
            true
        } else {
            permissionRequestAttempted = true
            permissionLauncher.launch(missing)
            false
        }
    }

    LaunchedEffect(uiState.authenticated) {
        if (uiState.authenticated) {
            viewModel.applyAuthenticatedDefaults()
            if (WardrivingPermissions.hasRequiredRuntimePermissions(context)) {
                route = "dashboard"
            } else if (!permissionRequestAttempted) {
                permissionRequestAttempted = true
                permissionLauncher.launch(WardrivingPermissions.missingRuntimePermissions(context))
            } else {
                route = "settings"
            }
        } else {
            permissionRequestAttempted = false
            route = "login"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RF Village MX Wardriving") },
                actions = {
                    if (uiState.authenticated && route != "splash") {
                        TextButton(onClick = { route = "dashboard" }) { Text("Dashboard") }
                        TextButton(onClick = { route = "settings" }) { Text("Settings") }
                    }
                },
            )
        },
        bottomBar = {
            if (route != "splash" && route != "login" && route != "register" && route != "recovery" && route != "sessionDetail" && route != "advanced") {
                FieldBottomNav(route) { route = it }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            StatusLine(uiState)
            when (route) {
                "splash" -> SplashScreen(
                    onContinue = {
                        permissionLauncher.launch(WardrivingPermissions.runtimePermissions())
                    },
                    onLogin = { route = "login" },
                )
                "login" -> LoginScreen(
                    title = "Login",
                    action = "Login",
                    onSubmit = viewModel::login,
                    onAlt = { route = "register" },
                    onRecovery = { route = "recovery" },
                )
                "register" -> LoginScreen(
                    title = "Register",
                    action = "Register",
                    onSubmit = viewModel::register,
                    onAlt = { route = "login" },
                    onRecovery = { route = "recovery" },
                )
                "recovery" -> RecoveryScreen(viewModel::recover)
                "dashboard" -> DashboardScreen(
                    uiState = uiState,
                    onStart = { if (requestMissingPermissions()) viewModel.startSession() },
                    onPause = viewModel::pauseSession,
                    onResume = viewModel::resumeSession,
                    onStop = viewModel::stopSession,
                    onLive = { route = "live" },
                    onExport = { route = "export" },
                    onAdvanced = { route = "advanced" },
                    onSessionDetail = {
                        selectedSession = it
                        viewModel.observeSessionDetailMapPins(it.id)
                        route = "sessionDetail"
                    },
                )
                "live" -> LiveScanScreen(
                    uiState,
                    onStart = { if (requestMissingPermissions()) viewModel.startSession() },
                    viewModel::pauseSession,
                    viewModel::resumeSession,
                    viewModel::stopSession,
                )
                "sessions" -> SessionsScreen(uiState.sessions) {
                    selectedSession = it
                    viewModel.observeSessionDetailMapPins(it.id)
                    route = "sessionDetail"
                }
                "sessionDetail" -> SessionDetailScreen(selectedSession, uiState.sessionDetailMapPins)
                "export" -> ExportUploadScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSaveAs = {
                        val fileName = uiState.lastExportPath?.substringAfterLast('/') ?: "wardriving_export.csv"
                        saveAsLauncher.launch(fileName)
                    },
                )
                "settings" -> SettingsScreen(
                    settings = uiState.settings,
                    hasRequiredPermissions = WardrivingPermissions.hasRequiredRuntimePermissions(context),
                    onGrantPermissions = { requestMissingPermissions() },
                    viewModel = viewModel,
                )
                "advanced" -> AdvancedAtModemScreen()
            }
        }
    }
}

@Composable
private fun FieldBottomNav(currentRoute: String, onRoute: (String) -> Unit) {
    val items = listOf(
        "dashboard" to "Dashboard",
        "live" to "Live",
        "sessions" to "Sessions",
        "export" to "Export",
        "settings" to "Settings",
    )
    NavigationBar {
        items.forEach { (route, label) ->
            val iconRes = when (route) {
                "dashboard" -> R.drawable.dashboard
                "live" -> R.drawable.live
                "sessions" -> R.drawable.sessions
                "export" -> R.drawable.export
                "settings" -> R.drawable.settings
                else -> null
            }
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = onClick@{
                    onRoute(route)
                },
                label = { Text(label) },
                icon = {
                    if (iconRes != null) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified,
                        )
                    } else {
                        Text(label.take(1))
                    }
                },
            )
        }
    }
}

@Composable
private fun SessionsScreen(
    sessions: List<WardrivingSessionEntity>,
    onSessionDetail: (WardrivingSessionEntity) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Sessions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (sessions.isEmpty()) {
            item { EmptyCard("No saved sessions yet.") }
        } else {
            items(sessions) { SessionRow(it, onSessionDetail) }
        }
    }
}

@Composable
private fun SplashScreen(onContinue: () -> Unit, onLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppLogo()
        Text(
            "This app collects nearby Wi-Fi/BLE identifiers, cellular network data, and GPS coordinates for wardriving/research purposes.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Location is required because Wi-Fi and BLE scan results are location-derived, GPS geopositions samples, and LTE cell samples need coordinates.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onContinue) { Text("Grant permissions") }
            OutlinedButton(onClick = onLogin) { Text("API login") }
        }
    }
}

@Composable
private fun LoginScreen(
    title: String,
    action: String,
    onSubmit: (String, String) -> Unit,
    onAlt: () -> Unit,
    onRecovery: () -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(identifier, { identifier = it }, label = { Text("Username or email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onSubmit(identifier, password) }) { Text(action) }
            OutlinedButton(onClick = onAlt) { Text(if (action == "Login") "Register" else "Login") }
            TextButton(onClick = onRecovery) { Text("Recover") }
        }
    }
}

@Composable
private fun RecoveryScreen(onRecover: (String) -> Unit) {
    var identifier by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Password recovery", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(identifier, { identifier = it }, label = { Text("Username or email") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onRecover(identifier) }) { Text("Send recovery request") }
    }
}

@Composable
private fun DashboardScreen(
    uiState: MainUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onLive: () -> Unit,
    onExport: () -> Unit,
    onAdvanced: () -> Unit,
    onSessionDetail: (WardrivingSessionEntity) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Field dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            FieldModeControls(onStart, onPause, onResume, onStop)
        }
        item {
            StatusChipRow(uiState)
        }
        item {
            CounterGrid(uiState)
        }
        item {
            MapPanel(
                title = "Quick map preview",
                pins = uiState.liveMapPins,
                gpsStatus = uiState.counters.gpsStatus,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.55f),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onLive) { Text("Live scan") }
                OutlinedButton(onClick = onExport) { Text("Export/upload") }
                OutlinedButton(onClick = onAdvanced) { Text("Advanced LTE/AT") }
            }
        }
        item { Text("Session history", style = MaterialTheme.typography.titleLarge) }
        if (uiState.sessions.isEmpty()) {
            item { EmptyCard("No sessions yet. Start a field session to collect samples.") }
        } else {
            items(uiState.sessions.take(5)) { SessionRow(it, onSessionDetail) }
        }
    }
}

@Composable
private fun LiveScanScreen(
    uiState: MainUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MapPanel(
                title = "Live wardriving map",
                pins = uiState.liveMapPins,
                gpsStatus = uiState.counters.gpsStatus,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.15f),
            )
        }
        item { FieldModeControls(onStart, onPause, onResume, onStop) }
        item { CounterGrid(uiState) }
        item { Text("Upload/export status: ${uiState.counters.uploadStatus}", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ExportUploadScreen(uiState: MainUiState, viewModel: MainViewModel, onSaveAs: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Export and upload", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::exportWifiBle) { Text("Export Wi-Fi/BLE CSV") }
            Button(onClick = viewModel::exportLte) { Text("Export LTE CSV") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::exportZip) { Text("Export ZIP") }
            OutlinedButton(onClick = viewModel::shareLastExport, enabled = uiState.lastExportPath != null) { Text("Share exported files") }
            OutlinedButton(onClick = onSaveAs, enabled = uiState.lastExportPath != null) { Text("Save As") }
        }
        Button(onClick = viewModel::uploadExports, enabled = uiState.sessions.isNotEmpty()) { Text("Upload exported files to API") }
        Text("Save As uses Android Storage Access Framework so the file can be written to Downloads or any selected document provider.")
        Text("Last export: ${uiState.lastExportPath ?: "None"}")
    }
}

@Composable
private fun SessionDetailScreen(session: WardrivingSessionEntity?, pins: List<MapPin>) {
    if (session == null) {
        Text("No session selected")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Session detail", style = MaterialTheme.typography.headlineSmall) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ID: ${session.id}", fontWeight = FontWeight.Bold)
                    Text("Status: ${session.status}")
                    Text("Started: ${formatDate(session.startedAt)}")
                    Text("Ended: ${session.endedAt?.let(::formatDate) ?: "Active"}")
                    Text("Device source: ${session.deviceSource}")
                    Text("Wi-Fi: ${session.wifiEnabled} | BLE: ${session.bleEnabled} | LTE: ${session.lteEnabled}")
                    Text("Uploaded: ${session.uploaded}")
                    Text("Local export path: ${session.localExportPath ?: "None"}")
                }
            }
        }
        item {
            MapPanel(
                title = "Session map",
                pins = pins,
                gpsStatus = if (pins.isEmpty()) "No GPS-backed samples" else "${pins.size} mapped samples",
                modifier = Modifier.fillMaxWidth().aspectRatio(1.25f),
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: SessionSettings,
    hasRequiredPermissions: Boolean,
    onGrantPermissions: () -> Unit,
    viewModel: MainViewModel,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
        if (!hasRequiredPermissions) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Permissions required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Location, wireless scanning, phone state, and notification permissions are needed before field scanning can start.")
                        Button(onClick = onGrantPermissions) {
                            Text("Grant permissions")
                        }
                    }
                }
            }
        }
        item { ToggleRow("Enable Wi-Fi scanning", settings.wifiEnabled) { viewModel.updateSettings { s -> s.copy(wifiEnabled = it) } } }
        item { ToggleRow("Enable BLE scanning", settings.bleEnabled) { viewModel.updateSettings { s -> s.copy(bleEnabled = it) } } }
        item { ToggleRow("Enable LTE scanning", settings.lteEnabled) { viewModel.updateSettings { s -> s.copy(lteEnabled = it) } } }
        item { ToggleRow("Enable API upload after session", settings.uploadAfterSession) { viewModel.updateSettings { s -> s.copy(uploadAfterSession = it) } } }
        item { ToggleRow("Enable local CSV export", settings.localCsvExport) { viewModel.updateSettings { s -> s.copy(localCsvExport = it) } } }
        item { ToggleRow("Keep screen awake", settings.keepScreenAwake) { viewModel.updateSettings { s -> s.copy(keepScreenAwake = it) } } }
        item { ToggleRow("Anonymize SSID", settings.anonymizeSsid) { viewModel.updateSettings { s -> s.copy(anonymizeSsid = it) } } }
        item { ToggleRow("Anonymize BLE name", settings.anonymizeBleName) { viewModel.updateSettings { s -> s.copy(anonymizeBleName = it) } } }
        item {
            OutlinedButton(onClick = { viewModel.updateSettings { SessionSettings() } }) {
                Text("Reset settings")
            }
        }
        item {
            OutlinedButton(onClick = viewModel::logout) {
                Text("Logout API token")
            }
        }
        item {
            OutlinedButton(onClick = viewModel::deleteAllLocalData) {
                Text("Delete all local data")
            }
        }
    }
}

@Composable
private fun AdvancedAtModemScreen() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Advanced LTE/AT modem settings", style = MaterialTheme.typography.headlineSmall) }
        item {
            Text("Internal phone LTE sampling uses public Android Telephony APIs only. Raw AT commands are not available for internal modems on stock Android.")
        }
        item {
            Text("ExternalAtModemModule is documented for optional USB OTG modems such as SIMCom SIM7600 and Quectel devices. Destructive commands and band locking must remain disabled unless the user explicitly enables advanced mode.")
        }
        item {
            Text("Example read-only commands: ATI, AT+CGMI, AT+CGMM, AT+CGMR, AT+CSQ, AT+COPS?, AT+CEREG?, AT+CPSI?, AT+QENG=\"servingcell\", AT+QNWINFO.")
        }
    }
}

@Composable
private fun FieldModeControls(onStart: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onStop: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onStart, modifier = Modifier.weight(1f).height(56.dp)) { Text("Start") }
        OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(56.dp)) { Text("Pause") }
        OutlinedButton(onClick = onResume, modifier = Modifier.weight(1f).height(56.dp)) { Text("Resume") }
        OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f).height(56.dp)) { Text("Stop") }
    }
}

@Composable
private fun CounterGrid(uiState: MainUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CounterCard("Wi-Fi APs", uiState.counters.wifiCount.toString(), Modifier.weight(1f))
            CounterCard("BLE devices", uiState.counters.bleCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CounterCard("LTE samples", uiState.counters.lteCount.toString(), Modifier.weight(1f))
            CounterCard("GPS", uiState.counters.gpsStatus, Modifier.weight(1f))
        }
        CounterCard("API upload", uiState.counters.uploadStatus, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CounterCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange)
        Text(label)
    }
}

@Composable
private fun StatusLine(uiState: MainUiState) {
    Text("Status: ${uiState.status}", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AppLogo() {
    Card(Modifier.size(88.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("RF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusChipRow(uiState: MainUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip("GPS", uiState.counters.gpsStatus)
        StatusChip("API", if (uiState.authenticated) "JWT stored" else "Offline")
        StatusChip("Scanner", uiState.activeSessionId?.let { "Active" } ?: "Idle")
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text("$label: $value", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(message)
        }
    }
}

@Composable
private fun MapPanel(title: String, pins: List<MapPin>, gpsStatus: String, modifier: Modifier = Modifier) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${pins.size}/100 pins", style = MaterialTheme.typography.labelMedium)
            }
            if (pins.isEmpty()) {
                Box(
                    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No GPS-backed samples yet. $gpsStatus", modifier = Modifier.padding(16.dp))
                }
            } else {
                MapLibreWardrivingMap(pins = pins, modifier = modifier)
            }
            MapLegend()
        }
    }
}

@Composable
private fun MapLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(Color(0xFF2ECC71), "Wi-Fi")
        LegendDot(Color(0xFF3498DB), "BLE")
        LegendDot(Color(0xFFF39C12), "LTE")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.Black, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MapLibreWardrivingMap(pins: List<MapPin>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = {
            mapView.getMapAsync { map ->
                map.setStyle(BuildConfig.MAPLIBRE_STYLE_URL) {
                    map.clear()
                    val iconFactory = IconFactory.getInstance(context)
                    pins.forEach { pin ->
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(pin.latitude, pin.longitude))
                                .title(pin.label)
                                .snippet("${pin.type.name} ${pin.rssiOrSignal?.let { "$it dBm" } ?: ""}".trim())
                                .icon(iconFactory.fromBitmap(pinBitmap(pin.type))),
                        )
                    }
                    pins.firstOrNull()?.let { newest ->
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(newest.latitude, newest.longitude), 15.0))
                    }
                }
            }
        },
    )
}

private fun pinBitmap(type: MapPinType): Bitmap {
    val color = when (type) {
        MapPinType.WIFI -> android.graphics.Color.rgb(46, 204, 113)
        MapPinType.BLE -> android.graphics.Color.rgb(52, 152, 219)
        MapPinType.LTE -> android.graphics.Color.rgb(243, 156, 18)
    }
    val bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(18f, 18f, 14f, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(18f, 18f, 14f, paint)
    return bitmap
}

@Composable
private fun SessionRow(session: WardrivingSessionEntity, onOpen: (WardrivingSessionEntity) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(session.id, fontWeight = FontWeight.Bold)
            Text("${session.status} started ${formatDate(session.startedAt)}")
            Text("Wi-Fi ${session.wifiEnabled} | BLE ${session.bleEnabled} | LTE ${session.lteEnabled}")
            TextButton(onClick = { onOpen(session) }) { Text("Open session detail") }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epochMillis))
