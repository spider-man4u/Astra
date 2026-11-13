package com.spider.astra.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.Screens
import com.spider.astra.ui.components.AstraOrb
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    repository: AssistantRepository,
    settings: AstraSettings
) {
    val state by repository.stateFlow.collectAsState()
    val userName by settings.userName.collectAsState(initial = null)
    val tasks by repository.tasksFlow.collectAsState()
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val isBusy = state == AssistantState.THINKING ||
        state == AssistantState.PLANNING || state == AssistantState.EXECUTING

    fun submitCommand(raw: String) {
        val clean = raw.trim()
        if (clean.isNotEmpty() && !isBusy) {
            scope.launch { repository.sendCommand(clean) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ASTRA", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "PERSONAL ASSISTANT  /  PHASE 1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screens.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Hey " + (userName?.takeIf { it.isNotBlank() } ?: "Spider") + ".",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "What would you like to work on?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1424)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AstraOrb(
                            state = state,
                            modifier = Modifier.size(172.dp),
                            size = 172.dp,
                            glowEnabled = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(stateColor(state), CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stateLabel(state),
                                color = stateColor(state),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (isBusy) "Working through your request…" else "Local demo mode · No device actions enabled",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Command console", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type a command for ASTRA…") },
                        singleLine = true,
                        enabled = !isBusy
                    )
                    Button(
                        onClick = {
                            submitCommand(command)
                            command = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = command.isNotBlank() && !isBusy
                    ) {
                        Text(if (isBusy) "Processing…" else "Send command")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Quick prompts", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { submitCommand("Show ASTRA status") }, enabled = !isBusy) {
                            Text("System status")
                        }
                        OutlinedButton(onClick = { submitCommand("Help me plan my day") }, enabled = !isBusy) {
                            Text("Plan my day")
                        }
                        OutlinedButton(onClick = { submitCommand("Help me focus for 25 minutes") }, enabled = !isBusy) {
                            Text("Focus session")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { navController.navigate(Screens.CHAT) }) { Text("Open chat") }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Text(
                        "No activity yet. Try one of the quick prompts above.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(tasks.take(5), key = { it.id }) { task -> TaskRow(task) }
            }

            item {
                Text(
                    "ASTRA is currently running with a simulated assistant. Real AI, voice and device controls are added in later phases.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: AssistantTask) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.command, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                Text(
                    task.result ?: task.error ?: "Demo task",
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                task.status.name.lowercase().replace('_', ' '),
                color = taskColor(task.status),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun stateLabel(state: AssistantState): String =
    state.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun stateColor(state: AssistantState): Color = when (state) {
    AssistantState.IDLE -> Color(0xFF22D3EE)
    AssistantState.LISTENING -> Color(0xFF3B82F6)
    AssistantState.THINKING, AssistantState.PLANNING -> Color(0xFF8B5CF6)
    AssistantState.EXECUTING, AssistantState.WAITING_FOR_PERMISSION -> Color(0xFFF59E0B)
    AssistantState.COMPLETED -> Color(0xFF10B981)
    AssistantState.ERROR -> Color(0xFFF87171)
}

private fun taskColor(status: TaskStatus): Color = when (status) {
    TaskStatus.COMPLETED -> Color(0xFF10B981)
    TaskStatus.FAILED -> Color(0xFFF87171)
    TaskStatus.EXECUTING, TaskStatus.PLANNING -> Color(0xFF22D3EE)
    else -> Color(0xFF94A3B8)
}
