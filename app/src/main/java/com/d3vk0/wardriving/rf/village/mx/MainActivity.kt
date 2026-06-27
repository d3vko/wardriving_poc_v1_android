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
import androidx.compose.foundation.Image
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.d3vk0.wardriving.rf.village.mx.core.domain.SessionFilter
import com.d3vk0.wardriving.rf.village.mx.core.domain.filterSessions
import com.d3vk0.wardriving.rf.village.mx.core.local.WardrivingSessionEntity
import com.d3vk0.wardriving.rf.village.mx.ui.theme.WardrivingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
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
    val snackbarHostState = remember { SnackbarHostState() }
    var route by remember { mutableStateOf(if (uiState.authenticated) AUTHENTICATED_START_ROUTE else "login") }
    var selectedSession by remember { mutableStateOf<WardrivingSessionEntity?>(null) }
    var permissionRequestAttempted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.all { it } && WardrivingPermissions.hasRequiredRuntimePermissions(context)
        route = if (granted) AUTHENTICATED_START_ROUTE else "settings"
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
                route = AUTHENTICATED_START_ROUTE
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

    LaunchedEffect(viewModel) {
        viewModel.uiErrors.collectLatest { event ->
            snackbarHostState.showSnackbar(
                message = event.message,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("RF Village MX Wardriving") },
            )
        },
        bottomBar = {
            if (route != "login" && route != "register" && route != "recovery" && route != "sessionDetail" && route != "advanced") {
                FieldBottomNav(route) { route = it }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            StatusLine(uiState)
            when (route) {
                "login" -> LoginScreen(
                    onSubmit = viewModel::login,
                    onRegister = { route = "register" },
                    onRecovery = { route = "recovery" },
                )
                "register" -> RegisterScreen(
                    onSubmit = viewModel::register,
                    onLogin = { route = "login" },
                    onRecovery = { route = "recovery" },
                )
                "recovery" -> RecoveryScreen(viewModel::recover)
                "dashboard" -> DashboardScreen(
                    uiState = uiState,
                    onAdvanced = { route = "advanced" },
                )
                "live" -> LiveScanScreen(
                    uiState,
                    onStart = { if (requestMissingPermissions()) viewModel.startSession() },
                    viewModel::pauseSession,
                    viewModel::resumeSession,
                    viewModel::stopSession,
                )
                "sessions" -> SessionsScreen(
                    sessions = uiState.sessions,
                    selectedFilter = uiState.sessionFilter,
                    onFilterChange = viewModel::setSessionFilter,
                ) {
                    selectedSession = it
                    viewModel.observeSessionDetailMapPins(it.id)
                    route = "sessionDetail"
                }
                "sessionDetail" -> SessionDetailScreen(
                    session = selectedSession,
                    pins = uiState.sessionDetailMapPins,
                    uploadState = uiState.sessionUploadState,
                    uploadInProgress = uiState.uploadingSessionId == selectedSession?.id,
                    onBack = { route = "sessions" },
                    onUpload = viewModel::uploadSession,
                )
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
    NavigationBar {
        FIELD_NAVIGATION_ITEMS.forEach { (route, label) ->
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
    selectedFilter: SessionFilter,
    onFilterChange: (SessionFilter) -> Unit,
    onSessionDetail: (WardrivingSessionEntity) -> Unit,
) {
    val visibleSessions = filterSessions(sessions, selectedFilter)
    val emptyMessage = when (selectedFilter) {
        SessionFilter.UNPROCESSED -> "No hay sesiones solo locales."
        SessionFilter.PROCESSED -> "No hay sesiones en plataforma."
        SessionFilter.ALL -> "No saved sessions yet."
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Sessions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SessionFilter.entries.forEach { filter ->
                    if (filter == selectedFilter) {
                        Button(
                            onClick = { onFilterChange(filter) },
                            modifier = Modifier.weight(1f),
                        ) { Text(filter.label) }
                    } else {
                        OutlinedButton(
                            onClick = { onFilterChange(filter) },
                            modifier = Modifier.weight(1f),
                        ) { Text(filter.label) }
                    }
                }
            }
        }
        if (visibleSessions.isEmpty()) {
            item { EmptyCard(emptyMessage) }
        } else {
            items(visibleSessions, key = WardrivingSessionEntity::id) { SessionRow(it, onSessionDetail) }
        }
    }
}

@Composable
private fun RegisterScreen(
    onSubmit: (String, String, String, String) -> Unit,
    onLogin: () -> Unit,
    onRecovery: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        errorMessage = when {
            username.isBlank() || email.isBlank() || password.isBlank() || passwordConfirm.isBlank() ->
                "Completa todos los campos"
            password != passwordConfirm ->
                "Las contraseñas no coinciden"
            else -> null
        }
        if (errorMessage == null) {
            onSubmit(username, email, password, passwordConfirm)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            passwordConfirm,
            { passwordConfirm = it },
            label = { Text("Confirm password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = ::submit) { Text("Register") }
            OutlinedButton(onClick = onLogin) { Text("Login") }
            TextButton(onClick = onRecovery) { Text("Recover") }
        }
    }
}

@Composable
private fun LoginScreen(
    onSubmit: (String, String) -> Unit,
    onRegister: () -> Unit,
    onRecovery: () -> Unit,
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(identifier, { identifier = it }, label = { Text("Username or email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onSubmit(identifier, password) }) { Text("Login") }
            OutlinedButton(onClick = onRegister) { Text("Register") }
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
    onAdvanced: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Field dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
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
        item { OutlinedButton(onClick = onAdvanced) { Text("Advanced LTE/AT") } }
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
        item { FieldModeControls(onStart, onPause, onResume, onStop, uiState) }
        item { CounterGrid(uiState) }
    }
}

@Composable
private fun ExportUploadScreen(uiState: MainUiState, viewModel: MainViewModel, onSaveAs: () -> Unit) {
    var selectedSessionId by remember(uiState.sessions) { mutableStateOf(uiState.sessions.firstOrNull()?.id) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Exportar sesión", style = MaterialTheme.typography.headlineSmall) }
        if (uiState.sessions.isEmpty()) {
            item { EmptyCard("No hay sesiones disponibles.") }
        } else {
            items(uiState.sessions) { session ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedSessionId == session.id, onClick = { selectedSessionId = session.id })
                    Text("${formatDate(session.startedAt)} · ${session.status}")
                }
            }
        }
        item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selectedSessionId?.let(viewModel::exportWifiBle) }, enabled = selectedSessionId != null) { Text("Wi-Fi/BLE CSV") }
            Button(onClick = { selectedSessionId?.let(viewModel::exportLte) }, enabled = selectedSessionId != null) { Text("LTE CSV") }
        }
        }
        item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selectedSessionId?.let(viewModel::exportZip) }, enabled = selectedSessionId != null) { Text("ZIP") }
            OutlinedButton(onClick = viewModel::shareLastExport, enabled = uiState.lastExportPath != null) { Text("Share exported files") }
            OutlinedButton(onClick = onSaveAs, enabled = uiState.lastExportPath != null) { Text("Save As") }
        }
        }
        item { Text("Save As uses Android Storage Access Framework so the file can be written to Downloads or any selected document provider.") }
        item { Text("Last export: ${uiState.lastExportPath ?: "None"}") }
    }
}

@Composable
private fun SessionDetailScreen(
    session: WardrivingSessionEntity?,
    pins: List<MapPin>,
    uploadState: com.d3vk0.wardriving.rf.village.mx.core.repository.SessionUploadState,
    uploadInProgress: Boolean,
    onBack: () -> Unit,
    onUpload: (String) -> Unit,
) {
    if (session == null) {
        Text("No session selected")
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Volver a Sessions") }
                Text("Session detail", style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ID: ${session.id}", fontWeight = FontWeight.Bold)
                    Text("Status: ${session.status}")
                    Text("Started: ${formatDate(session.startedAt)}")
                    Text("Ended: ${session.endedAt?.let(::formatDate) ?: "Active"}")
                    Text("Device source: ${session.deviceSource}")
                    Text("Wi-Fi: ${session.wifiEnabled} | BLE: ${session.bleEnabled} | LTE: ${session.lteEnabled}")
                    Text("Upload: ${uploadState.label}")
                    Text("Wi-Fi/BLE: ${uploadState.wifiBleLabel}")
                    Text("LTE: ${uploadState.lteLabel}")
                    Text("Local export path: ${session.localExportPath ?: "None"}")
                    Button(
                        onClick = { onUpload(session.id) },
                        enabled = uploadState.canUpload && !uploadInProgress && session.status == "STOPPED",
                    ) {
                        Text(
                            when {
                                uploadInProgress -> "Subiendo…"
                                uploadState.label == "Procesando" -> "Procesando en API"
                                else -> "Subir sesión"
                            },
                        )
                    }
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
        item { ToggleRow("Enable local CSV export", settings.localCsvExport) { viewModel.updateSettings { s -> s.copy(localCsvExport = it) } } }
        item { ToggleRow("Keep screen awake", settings.keepScreenAwake) { viewModel.updateSettings { s -> s.copy(keepScreenAwake = it) } } }
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
private fun FieldModeControls(
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    uiState: MainUiState,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onStart, enabled = uiState.activeSessionId == null && !uiState.isStopping, modifier = Modifier.weight(1f).height(56.dp)) { Text("Start") }
        OutlinedButton(onClick = onPause, enabled = uiState.activeSessionId != null && !uiState.isStopping, modifier = Modifier.weight(1f).height(56.dp)) { Text("Pause") }
        OutlinedButton(onClick = onResume, enabled = uiState.activeSessionId != null && !uiState.isStopping, modifier = Modifier.weight(1f).height(56.dp)) { Text("Resume") }
        OutlinedButton(onClick = onStop, enabled = uiState.activeSessionId != null && !uiState.isStopping, modifier = Modifier.weight(1f).height(56.dp)) { Text(if (uiState.isStopping) "Stopping…" else "Stop") }
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
    Image(
        painter = painterResource(R.mipmap.ic_launcher),
        contentDescription = "RF Village MX Wardriving",
        modifier = Modifier
            .size(88.dp)
            .clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Crop,
    )
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
    var expanded by remember { mutableStateOf(false) }
    val controller = remember { WardrivingMapController() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${pins.size} mapped samples", style = MaterialTheme.typography.labelMedium)
            }
            if (pins.isEmpty()) {
                Box(
                    modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No GPS-backed samples yet. $gpsStatus", modifier = Modifier.padding(16.dp))
                }
            } else {
                Box {
                    MapLibreWardrivingMap(
                        pins = pins,
                        controller = controller,
                        modifier = if (expanded) Modifier.fillMaxWidth().height(520.dp) else modifier,
                    )
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedButton(onClick = controller::zoomIn) { Text("+") }
                        OutlinedButton(onClick = controller::zoomOut) { Text("−") }
                        OutlinedButton(onClick = controller::centerNewest) { Text("Reciente") }
                        OutlinedButton(onClick = controller::fitAll) { Text("Ver todos") }
                        OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Reducir" else "Ampliar") }
                    }
                }
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

private class WardrivingMapController {
    var zoomInAction: () -> Unit = {}
    var zoomOutAction: () -> Unit = {}
    var centerNewestAction: () -> Unit = {}
    var fitAllAction: () -> Unit = {}
    fun zoomIn() = zoomInAction()
    fun zoomOut() = zoomOutAction()
    fun centerNewest() = centerNewestAction()
    fun fitAll() = fitAllAction()
}

@Composable
private fun MapLibreWardrivingMap(
    pins: List<MapPin>,
    controller: WardrivingMapController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var styleRequested by remember { mutableStateOf(false) }
    var styleLoaded by remember { mutableStateOf(false) }
    var cameraInitialized by remember { mutableStateOf(false) }
    SideEffect {
        controller.zoomInAction = {
            mapView.getMapAsync { map -> map.animateCamera(CameraUpdateFactory.zoomIn()) }
        }
        controller.zoomOutAction = {
            mapView.getMapAsync { map -> map.animateCamera(CameraUpdateFactory.zoomOut()) }
        }
        controller.centerNewestAction = {
            pins.firstOrNull()?.let { newest ->
                mapView.getMapAsync { map ->
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(newest.latitude, newest.longitude), 15.0))
                }
            }
        }
        controller.fitAllAction = {
            mapView.getMapAsync { map -> map.fitPins(pins) }
        }
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
    LaunchedEffect(pins, styleLoaded) {
        if (!styleLoaded) return@LaunchedEffect
        mapView.getMapAsync { map ->
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
            if (!cameraInitialized) {
                cameraInitialized = true
                pins.firstOrNull()?.let { newest ->
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(newest.latitude, newest.longitude), 15.0))
                }
            }
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = {
            mapView.getMapAsync { map ->
                map.uiSettings.apply {
                    isScrollGesturesEnabled = true
                    isZoomGesturesEnabled = true
                    isCompassEnabled = true
                }
                if (!styleRequested) {
                    styleRequested = true
                    map.setStyle(BuildConfig.MAPLIBRE_STYLE_URL) {
                        styleLoaded = true
                    }
                }
            }
        },
    )
}

private fun org.maplibre.android.maps.MapLibreMap.fitPins(pins: List<MapPin>) {
    if (pins.isEmpty()) return
    if (pins.size == 1) {
        animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(pins[0].latitude, pins[0].longitude), 15.0))
        return
    }
    val bounds = LatLngBounds.Builder()
        .includes(pins.map { LatLng(it.latitude, it.longitude) })
        .build()
    animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
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
