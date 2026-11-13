package com.spider.astra.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.data.settings.AstraSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController, repository: AssistantRepository, settings: AstraSettings) {
    val state by repository.stateFlow.collectAsState(initial = AssistantState.IDLE)
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val messages = repository.messages()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Conversation") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.padding(end = 8.dp).background(stateColor(state), CircleShape).size(10.dp))
                Text(state.name.lowercase(), color = stateColor(state))
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
                items(messages, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = if (message.direction == AssistantMessage.Direction.USER) Arrangement.End else Arrangement.Start) {
                        Text(message.text, modifier = Modifier.background(
                            if (message.direction == AssistantMessage.Direction.USER) Color(0xFF15304A) else Color(0xFF101C30),
                            RoundedCornerShape(14.dp)).padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(value = command, onValueChange = { command = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Ask ASTRA...") }, singleLine = true)
                Button(onClick = {
                    val text = command.trim()
                    if (text.isNotEmpty()) {
                        command = ""
                        scope.launch { repository.sendCommand(text) }
                    }
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
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