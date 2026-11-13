package com.spider.astra.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.ui.theme.AstraPalette
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavHostController, settings: AstraSettings) {
    val storedName by settings.userName.collectAsState(initial = null)
    val voiceEnabled by settings.voiceEnabled.collectAsState(initial = false)
    val themeMode by settings.themeMode.collectAsState(initial = "system")
    var nameInput by remember { mutableStateOf("Spider") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(storedName) {
        nameInput = storedName?.takeIf { it.isNotBlank() } ?: "Spider"
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

                SettingsPanel(title = "VOICE INTERFACE", subtitle = "Preference only · Voice arrives in Phase 4") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Voice responses", color = AstraPalette.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Save your preference for the future voice engine.", color = AstraPalette.Muted, fontSize = 11.sp)
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
                }

                SettingsPanel(title = "RUNTIME", subtitle = "Current assistant environment") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(AstraPalette.Amber, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("LOCAL SIMULATION", color = AstraPalette.Amber, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "This build simulates command processing. No AI provider, Termux backend, microphone, floating overlay or device-control service is connected.",
                        color = AstraPalette.Muted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }

                SettingsPanel(title = "ROADMAP", subtitle = "What comes after the foundation") {
                    RoadmapLine("02", "Floating orb", "Persistent overlay and permission flow", true)
                    RoadmapLine("03", "Background tasks", "Foreground service and task handling", false)
                    RoadmapLine("04", "Voice interface", "Speech recognition and spoken responses", false)
                    RoadmapLine("05", "Core intelligence", "Secure connection to your Termux backend", false)
                }

                Text(
                    "ASTRA  /  PERSONAL INTELLIGENCE  /  BUILD 01",
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
