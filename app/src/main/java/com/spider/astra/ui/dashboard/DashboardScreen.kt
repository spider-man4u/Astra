package com.spider.astra.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.Screens
import com.spider.astra.ui.components.AstraOrb
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, repository: AssistantRepository, settings: AstraSettings) {
    val state by repository.stateFlow.collectAsState(initial = AssistantState.IDLE)
    val userName by settings.userName.collectAsState(initial = "Spider")
    val recentTasks = repository.recentTasks(5)
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        TopAppBar(title = { Text("ASTRA") }, actions = {
            IconButton(onClick = { navController.navigate(Screens.SETTINGS) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hey ${userName ?: "Spider"}. What are we doing today?", modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(stateColor(state), CircleShape))
                Text("  ${state.name.lowercase()}", color = stateColor(state))
            }
            Spacer(Modifier.height(20.dp))
            AstraOrb(state = state, modifier = Modifier.size(160.dp), size = 160.dp)
            Spacer(Modifier.height(22.dp))
            TextField(value = command, onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tell ASTRA what to do...") }, singleLine = true)
            Button(onClick = {
                val text = command.trim()
                if (text.isNotEmpty()) {
                    command = ""
                    scope.launch { repository.sendCommand(text) }
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Send command") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 18.dp))
                TextButton(onClick = { navController.navigate(Screens.CHAT) }) { Text("Open chat") }
            }
            if (recentTasks.isEmpty()) Text("No recent tasks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(Modifier.fillMaxWidth()) {
                items(recentTasks, key = { it.id }) { task ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(task.command, maxLines = 1)
                            Text(task.status.name.lowercase(), color = taskColor(task.status))
                        }
                    }
                }
            }
        }
    }
}

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