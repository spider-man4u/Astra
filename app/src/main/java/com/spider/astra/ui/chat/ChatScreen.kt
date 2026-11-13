package com.spider.astra.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController, repository: AssistantRepository) {
    val state by repository.stateFlow.collectAsState()
    val messages by repository.messagesFlow.collectAsState()
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isBusy = state == AssistantState.THINKING ||
        state == AssistantState.PLANNING || state == AssistantState.EXECUTING

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun send() {
        val text = command.trim()
        if (text.isNotEmpty() && !isBusy) {
            command = ""
            scope.launch { repository.sendCommand(text) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Conversation")
                        Text(
                            "Local demo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message ASTRA…") },
                        singleLine = true,
                        enabled = !isBusy
                    )
                    IconButton(
                        onClick = { send() },
                        enabled = command.isNotBlank() && !isBusy
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(state.name.lowercase().replace('_', ' ')) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "No real-world actions are performed in this demo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message -> MessageBubble(message) }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage) {
    val isUser = message.direction == AssistantMessage.Direction.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp),
            color = if (isUser) Color(0xFF15304A) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(
                    if (isUser) "YOU" else "ASTRA",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) Color(0xFF7DD3FC) else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(message.text, color = MaterialTheme.colorScheme.onSurface)
                if (message.state == AssistantMessage.AssistantMessageState.FAILED) {
                    Text(
                        "Not completed",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
