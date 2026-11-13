package com.spider.astra.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.spider.astra.bridge.BridgeTokenStore
import com.spider.astra.bridge.TermuxBridgeClient
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.services.overlay.AstraOverlayService
import com.spider.astra.ui.theme.AstraPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(navController: NavHostController, settings: AstraSettings) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val storedName by settings.userName.collectAsState(initial = null)
    val voiceEnabled by settings.voiceEnabled.collectAsState(initial = false)
    val voiceLanguage by settings.voiceLanguage.collectAsState(initial = "en-IN")
    val themeMode by settings.themeMode.collectAsState(initial = "system")
    var nameInput by remember { mutableStateOf("Spider") }
    var overlayGranted by remember { mutableStateOf(AndroidSettings.canDrawOverlays(context)) }
    var serviceRunning by remember { mutableStateOf(AstraOverlayService.isRunning) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val bridgeTokenStore = remember(context) { BridgeTokenStore(context) }
    val bridgeClient = remember(bridgeTokenStore) { TermuxBridgeClient(bridgeTokenStore) }
    var bridgeTokenInput by remember { mutableStateOf("") }
    var bridgeConfigured by remember { mutableStateOf(false) }
    var bridgeBusy by remember { mutableStateOf(false) }
    var bridgeStatus by remember { mutableStateOf("Checking saved connection…") }

    LaunchedEffect(Unit) {
        bridgeConfigured = withContext(Dispatchers.IO) { bridgeTokenStore.hasToken() }
        bridgeStatus = if (bridgeConfigured) {
            "Token is saved. Test the connection to check whether Termux is running."
        } else {
            "Not configured · ASTRA is using its local safe-action engine."
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Notification permission is optional for starting an FGS. Android may
        // still show the service in its active-app/task-manager surfaces.
        AstraOverlayService.start(context)
        serviceRunning = true
        scope.launch {
            snackbarHostState.showSnackbar(
                if (it) "Floating orb started" else "Orb started; notification drawer alerts are disabled"
            )
        }
    }

    fun startOrb() {
        overlayGranted = AndroidSettings.canDrawOverlays(context)
        if (!overlayGranted) {
            val permissionIntent = Intent(
                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)
            )
            context.startActivity(permissionIntent)
            return
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AstraOverlayService.start(context)
            serviceRunning = true
            scope.launch { snackbarHostState.showSnackbar("Floating orb started") }
        }
    }

    LaunchedEffect(storedName) {
        nameInput = storedName?.takeIf { it.isNotBlank() } ?: "Spider"
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = AndroidSettings.canDrawOverlays(context)
                serviceRunning = AstraOverlayService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(AstraPalette.Background, Color(0xFF071426), AstraPalette.BackgroundDeep))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(AstraPalette.Panel)
                            .border(1.dp, AstraPalette.Border, CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AstraPalette.Cyan)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SYSTEM CONFIG", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                        Text("Preferences", color = AstraPalette.Text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(AstraPalette.PanelRaised), contentAlignment = Alignment.Center) {
                        Text("✦", color = AstraPalette.Cyan, fontSize = 18.sp)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("TUNE YOUR EXPERIENCE", color = AstraPalette.Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text("Make it yours.", color = AstraPalette.Text, fontSize = 29.sp, fontWeight = FontWeight.Light)
                    Text("Personal preferences for your ASTRA interface.", color = AstraPalette.Muted, fontSize = 12.sp)
                }

                SettingsPanel(title = "IDENTITY", subtitle = "How ASTRA addresses you") {
                    Text("DISPLAY NAME", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Your name") },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstraPalette.Cyan,
                            unfocusedBorderColor = AstraPalette.Border,
                            focusedTextColor = AstraPalette.Text,
                            unfocusedTextColor = AstraPalette.Text,
                            cursorColor = AstraPalette.Cyan
                        )
                    )
                    Button(
                        onClick = {
                            val clean = nameInput.trim().ifBlank { "Spider" }
                            nameInput = clean
                            scope.launch {
                                settings.updateUserName(clean)
                                snackbarHostState.showSnackbar("Display name saved")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AstraPalette.CyanBright, contentColor = AstraPalette.BackgroundDeep)
                    ) {
                        Text("SAVE IDENTITY", fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                SettingsPanel(title = "FLOATING INTERFACE", subtitle = "Keep the ASTRA orb available over other apps") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(if (overlayGranted) AstraPalette.Green else AstraPalette.Amber, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overlayGranted) "OVERLAY ACCESS GRANTED" else "OVERLAY ACCESS REQUIRED",
                            color = if (overlayGranted) AstraPalette.Green else AstraPalette.Amber,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "ASTRA needs your explicit Android permission to draw a small floating orb above other apps. It can be moved, minimized or stopped at any time.",
                        color = AstraPalette.Muted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                    Button(
                        onClick = {
                            val intent = Intent(
                                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName)
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (overlayGranted) AstraPalette.PanelRaised else AstraPalette.CyanBright,
                            contentColor = if (overlayGranted) AstraPalette.Text else AstraPalette.BackgroundDeep
                        )
                    ) {
                        Text(
                            if (overlayGranted) "MANAGE OVERLAY PERMISSION" else "GRANT OVERLAY ACCESS",
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            if (serviceRunning) {
                                AstraOverlayService.stop(context)
                                serviceRunning = false
                                scope.launch { snackbarHostState.showSnackbar("Floating orb stopped") }
                            } else {
                                startOrb()
                            }
                        },
                        enabled = overlayGranted || serviceRunning,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serviceRunning) AstraPalette.Red.copy(alpha = 0.18f) else AstraPalette.CyanBright,
                            contentColor = if (serviceRunning) AstraPalette.Red else AstraPalette.BackgroundDeep,
                            disabledContainerColor = AstraPalette.PanelRaised,
                            disabledContentColor = AstraPalette.Dim
                        )
                    ) {
                        Text(
                            if (serviceRunning) "STOP FLOATING ORB" else "START FLOATING ORB",
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (serviceRunning) "The floating interface is running. Use its × control or this button to stop it."
                        else "The orb starts only after you grant overlay access and explicitly enable it.",
                        color = AstraPalette.Dim,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                SettingsPanel(title = "APPEARANCE", subtitle = "Choose the visual atmosphere") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ThemeChoice("SYSTEM", themeMode == "system" || themeMode == null, Modifier.weight(1f)) {
                            scope.launch { settings.updateThemeMode("system") }
                        }
                        ThemeChoice("DARK", themeMode == "dark", Modifier.weight(1f)) {
                            scope.launch { settings.updateThemeMode("dark") }
                        }
                        ThemeChoice("LIGHT", themeMode == "light", Modifier.weight(1f)) {
                            scope.launch { settings.updateThemeMode("light") }
                        }
                    }
                    Text("System follows your Android appearance setting.", color = AstraPalette.Dim, fontSize = 10.sp)
                }

                SettingsPanel(title = "VOICE INTERFACE", subtitle = "Tap-to-talk input and spoken replies") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Speak assistant replies", color = AstraPalette.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Automatically read replies after a voice command. You can also tap the speaker on any ASTRA message.", color = AstraPalette.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { enabled -> scope.launch { settings.setVoiceEnabled(enabled) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AstraPalette.BackgroundDeep,
                                checkedTrackColor = AstraPalette.Cyan,
                                uncheckedThumbColor = AstraPalette.Muted,
                                uncheckedTrackColor = AstraPalette.PanelRaised
                            )
                        )
                    }
                    Text("VOICE INPUT LANGUAGE", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ThemeChoice("ENGLISH · IN", voiceLanguage == "en-IN", Modifier.weight(1f)) {
                            scope.launch { settings.setVoiceLanguage("en-IN") }
                        }
                        ThemeChoice("HINDI · IN", voiceLanguage == "hi-IN", Modifier.weight(1f)) {
                            scope.launch { settings.setVoiceLanguage("hi-IN") }
                        }
                    }
                    Text(
                        "Microphone access is requested only when you tap the mic in Chat. ASTRA does not listen in the background.",
                        color = AstraPalette.Dim,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                SettingsPanel(title = "TERMUX CORE LINK", subtitle = "Connect ASTRA to your Python brain on this device") {
                    Text("LOCAL ENDPOINT", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                    Text(
                        "http://127.0.0.1:8765",
                        color = AstraPalette.Cyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Start bridge_server.py in Termux, then paste its access token here. The token is encrypted with Android Keystore.",
                        color = AstraPalette.Muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = bridgeTokenInput,
                        onValueChange = { if (it.length <= 256) bridgeTokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Termux bridge access token") },
                        placeholder = { Text("Paste token from Termux") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstraPalette.Cyan,
                            unfocusedBorderColor = AstraPalette.Border,
                            focusedTextColor = AstraPalette.Text,
                            unfocusedTextColor = AstraPalette.Text,
                            cursorColor = AstraPalette.Cyan
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val token = bridgeTokenInput.trim()
                                scope.launch {
                                    bridgeBusy = true
                                    val saved = withContext(Dispatchers.IO) {
                                        runCatching { bridgeTokenStore.saveToken(token) }
                                    }
                                    if (saved.isSuccess) {
                                        bridgeTokenInput = ""
                                        bridgeConfigured = true
                                        bridgeStatus = "Token saved securely. Checking Termux…"
                                        val health = bridgeClient.checkHealth()
                                        bridgeStatus = health.message
                                    } else {
                                        bridgeStatus = saved.exceptionOrNull()?.message ?: "Could not save token."
                                    }
                                    bridgeBusy = false
                                }
                            },
                            enabled = bridgeTokenInput.trim().length >= 32 && !bridgeBusy,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AstraPalette.CyanBright,
                                contentColor = AstraPalette.BackgroundDeep
                            )
                        ) {
                            Text("SAVE TOKEN", fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    bridgeBusy = true
                                    bridgeStatus = bridgeClient.checkHealth().message
                                    bridgeBusy = false
                                }
                            },
                            enabled = bridgeConfigured && !bridgeBusy,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text("TEST LINK", fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(7.dp).background(
                                if (bridgeStatus.startsWith("Connected")) AstraPalette.Green
                                else if (bridgeConfigured) AstraPalette.Amber else AstraPalette.Dim,
                                CircleShape
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            bridgeStatus,
                            color = if (bridgeStatus.startsWith("Connected")) AstraPalette.Green else AstraPalette.Muted,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { bridgeTokenStore.clear() }
                                bridgeConfigured = false
                                bridgeTokenInput = ""
                                bridgeStatus = "Token removed. Commands will use local demo mode."
                            }
                        },
                        enabled = bridgeConfigured && !bridgeBusy,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("DISCONNECT & ERASE TOKEN", color = AstraPalette.Red, fontSize = 9.sp, letterSpacing = 0.8.sp)
                    }
                }

                SettingsPanel(title = "RUNTIME", subtitle = "Current assistant environment") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(AstraPalette.Amber, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("LOCAL SIMULATION", color = AstraPalette.Amber, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "When a Termux token is configured, chat commands use the existing Python assistant. Provider keys remain in Termux. Without a saved token, ASTRA uses its local safe-action demo.",
                        color = AstraPalette.Muted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }

                SettingsPanel(title = "ROADMAP", subtitle = "What comes after the foundation") {
                    RoadmapLine("02", "Floating orb", "Overlay and permission flow", true)
                    RoadmapLine("03", "Background tasks", "Foreground service and task handling", true)
                    RoadmapLine("04", "Voice interface", "Tap-to-talk and spoken responses", true)
                    RoadmapLine("05", "Core intelligence", "Authenticated Termux backend bridge", true)
                }

                Text(
                    "ASTRA  /  PERSONAL INTELLIGENCE  /  BUILD 05",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = AstraPalette.Dim,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AstraPalette.Panel.copy(alpha = 0.94f),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = AstraPalette.Cyan, fontSize = 9.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = AstraPalette.Muted, fontSize = 11.sp)
            }
            content()
        }
    }
}

@Composable
private fun ThemeChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        color = if (selected) AstraPalette.Cyan.copy(alpha = 0.15f) else AstraPalette.PanelSoft,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) AstraPalette.Cyan.copy(alpha = 0.7f) else AstraPalette.Border
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) AstraPalette.Cyan else AstraPalette.Muted,
                fontSize = 9.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RoadmapLine(number: String, title: String, detail: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(if (active) AstraPalette.Cyan.copy(alpha = 0.15f) else AstraPalette.PanelRaised)
                .border(1.dp, if (active) AstraPalette.Cyan.copy(alpha = 0.65f) else AstraPalette.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = if (active) AstraPalette.Cyan else AstraPalette.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = AstraPalette.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = AstraPalette.Muted, fontSize = 10.sp)
        }
    }
}
