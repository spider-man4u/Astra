package com.spider.astra.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spider.astra.R
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.Screens
import com.spider.astra.ui.components.AstraOrb
import com.spider.astra.ui.theme.AstraTheme
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController

/**
 * Main ASTRA dashboard.
 *
 * Layout:
 *   - Top app bar: ASTRA identity, current status, settings shortcut
 *   - Greeting line
 *   - Central animated orb
 *   - Command input (text field + send)
 *   - Recent tasks / activity panel
 *   - Quick actions row
 *
 * Tablet-first: the column is centred with comfortable spacing and stays readable
 * on large displays while remaining usable on phones.
 */
@Composable
fun DashboardScreen(
    navController: NavHostController,
    repository: AssistantRepository,
    settings: AstraSettings
) {
    AstraTheme {
        val context = LocalContext.current
        val state by repository.stateFlow().collectAsState()
        val messages = repository.messages()
        val recentTasks = repository.recentTasks(5)

        var command by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

        // Greeting observes the stored user name, defaulting to "Spider".
        val userName by settings.userName.collectAsState(initial = "Spider")

        // Status text and colour derive from the repository state.
        val statusText = stateText(state)
        val statusColor = stateColor(state)

        // Send action: hand the command to the mock repository and clear input.
        val onSend = {
            val text = command.trim()
            if (text.isEmpty()) return@launch
            command = ""
            coroutineScope.launch {
                repository.sendCommand(text)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color(0xFF050A14)
                    ),
                    actions = {
                        IconButton(onClick = { navController.navigate(Screens.SETTINGS) }) {
                            androidx.compose.material3.Icon(
                                imageVector = Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Greeting.
                Text(
                    text = "Hey ${userName ?: "Spider"}. What are we doing today?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Status chip row.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, shape = CircleShape)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Central orb (tablet-friendly: centred, generous size).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AstraOrb(
                        state = state,
                        modifier = Modifier.size(96.dp),
                        glowEnabled = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Command input.
                TextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(text = stringResource(id = R.string.command_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF0B1424),
                        focusedContainerColor = Color(0xFF101C30),
                        unfocusedContainerColor = Color(0xFF0B1424)
                    )
                )

                Button(
                    onClick = onSend,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Send,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(id = R.string.send))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Recent tasks / activity panel.
                Text(
                    text = stringResource(id = R.string.activity_panel),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.lazy.PaddingValues(start = 4.dp)
                ) {
                    items(items = recentTasks, key = { it.id }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = Color(0xFF0B1424)
                            )
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = androidx.compose.foundation.layout.Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(taskColor(task.status), shape = CircleShape)
                                )
                                androidx.compose.foundation.layout.Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp)
                                ) {
                                    Text(
                                        text = task.command.take(60),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = task.status.name.lowercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = taskStatusLabel(task.status),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = taskColor(task.status)
                                )
                            }
                        }
                    }
                }

                if (recentTasks.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_recent_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick actions.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAction("Ask ASTRA") {
                        navController.navigate(Screens.CHAT)
                    }
                    quickAction("Device status") { /* Phase 6 */ }
                    quickAction("Open applications") { /* Phase 6 */ }
                    quickAction("Memory") { /* Phase 9 */ }
                }
            }
        }
    }
}

private fun stateText(state: AssistantState): String = when (state) {
    AssistantState.IDLE -> "Idle"
    AssistantState.LISTENING -> "Listening"
    AssistantState.THINKING -> "Thinking"
    AssistantState.PLANNING -> "Planning"
    AssistantState.EXECUTING -> "Executing"
    AssistantState.WAITING_FOR_PERMISSION -> "Waiting for permission"
    AssistantState.COMPLETED -> "Completed"
    AssistantState.ERROR -> "Error"
}

private fun stateColor(state: AssistantState): Color = when (state) {
    AssistantState.IDLE -> Color(0xFF22D3EE)
    AssistantState.LISTENING -> Color(0xFF3B82F6)
    AssistantState.THINKING -> Color(0xFF8B5CF6)
    AssistantState.PLANNING -> Color(0xFFF59E0B)
    AssistantState.EXECUTING -> Color(0xFFEF4444)
    AssistantState.WAITING_FOR_PERMISSION -> Color(0xFFF59E0B)
    AssistantState.COMPLETED -> Color(0xFF10B981)
    AssistantState.ERROR -> Color(0xFFF87171)
}

private fun taskColor(status: TaskStatus): Color = when (status) {
    TaskStatus.COMPLETED -> Color(0xFF10B981)
    TaskStatus.FAILED -> Color(0xFFF87171)
    TaskStatus.EXECUTING, TaskStatus.PLANNING -> Color(0xFF22D3EE)
    else -> Color(0xFF94A3B8)
}

private fun taskStatusLabel(status: TaskStatus): String = status.name.lowercase()

private fun quickAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = label)
    }
}
