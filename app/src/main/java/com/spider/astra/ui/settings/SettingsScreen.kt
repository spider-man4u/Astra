package com.spider.astra.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.spider.astra.data.settings.AstraSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Personalisation", style = MaterialTheme.typography.titleLarge)
                Text("Make ASTRA feel a little more like yours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Display name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("What should ASTRA call you?") }
                    )
                    Button(
                        onClick = {
                            val cleanName = nameInput.trim().ifBlank { "Spider" }
                            nameInput = cleanName
                            scope.launch {
                                settings.updateUserName(cleanName)
                                snackbarHostState.showSnackbar("Display name saved")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save name") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose how ASTRA looks. System follows your Android setting.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = themeMode == "system" || themeMode == null,
                            onClick = { scope.launch { settings.updateThemeMode("system") } },
                            label = { Text("System") }
                        )
                        FilterChip(
                            selected = themeMode == "dark",
                            onClick = { scope.launch { settings.updateThemeMode("dark") } },
                            label = { Text("Dark") }
                        )
                        FilterChip(
                            selected = themeMode == "light",
                            onClick = { scope.launch { settings.updateThemeMode("light") } },
                            label = { Text("Light") }
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Voice preference", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Saved preference only. Microphone and speech features arrive in Phase 4.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { enabled -> scope.launch { settings.setVoiceEnabled(enabled) } }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Assistant runtime", style = MaterialTheme.typography.titleMedium)
                    Text("Local mock mode", color = MaterialTheme.colorScheme.primary)
                    Text(
                        "The current app simulates command processing. No AI provider, Termux backend, overlay, microphone, or device-control service is connected.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Coming next", style = MaterialTheme.typography.titleMedium)
                    Text("Phase 2 · Floating bubble and overlay permission")
                    Text("Phase 3 · Background service and task handling")
                    Text("Phase 4 · Voice input and speech output")
                    Text("Phase 5 · Secure connection to the Termux backend")
                }
            }
        }
    }
}
