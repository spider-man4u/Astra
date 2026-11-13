package com.spider.astra.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spider.astra.R
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.Screens
import com.spider.astra.ui.theme.AstraTheme
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController

/**
 * Simple text conversation screen.
 *
 * Phase 1: text-only conversation backed by [MockAssistantRepository]. Voice is
 * added in Phase 4. The user can edit the text before sending (the field is the
 * editor), and ASTRA always replies honestly — it never fabricates capabilities.
 */
@Composable
fun ChatScreen(
    navController: NavHostController,
    repository: AssistantRepository,
    settings: AstraSettings
) {
    AstraTheme {
        val state by repository.stateFlow().collectAsState()
        val messages = repository.messages()

        var command by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()

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
                    title = { Text(text = "Conversation") },
                    colors = androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color(0xFF050A14)
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            androidx.compose.material3.Icon(
                                imageVector = ArrowBack,
                                contentDescription = "Back"
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
                // Status chip.
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(10.dp).background(stateColor(state), shape = CircleShape)
                    )
                    Text(
                        text = stateText(state),
                        style = MaterialTheme.typography.labelLarge,
                        color = stateColor(state),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Message history.
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(items = messages, key = { it.id }) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = if (msg.direction == AssistantMessage.Direction.USER) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            }
                        ) {
                            val bubbleColor = if (msg.direction == AssistantMessage.Direction.USER) {
                                Color(0xFF101C30)
                            } else {
                                Color(0xFF0B1424)
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .background(bubbleColor, shape = RoundedCornerShape(16.dp))
                                        .padding(
                                            PaddingValues(
                                                start = if (msg.direction == AssistantMessage.Direction.USER) 4.dp else 16.dp,
                                                top = 4.dp,
                                                end = if (msg.direction == AssistantMessage.Direction.USER) 16.dp else 4.dp,
                                                bottom = 4.dp
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Input area.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text(text = stringResource(id = R.string.command_hint)) },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            backgroundColor = Color(0xFF0B1424),
                            focusedContainerColor = Color(0xFF101C30),
                            unfocusedContainerColor = Color(0xFF0B1424)
                        )
                    )
                    Button(onClick = onSend) {
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
                }
            }
        }
    }
}

private fun stateColor(state: AssistantState): Color =
    when (state) {
        AssistantState.IDLE -> Color(0xFF22D3EE)
        AssistantState.LISTENING -> Color(0xFF3B82F6)
        AssistantState.THINKING -> Color(0xFF8B5CF6)
        AssistantState.PLANNING -> Color(0xFFF59E0B)
        AssistantState.EXECUTING -> Color(0xFFEF4444)
        AssistantState.WAITING_FOR_PERMISSION -> Color(0xFFF59E0B)
        AssistantState.COMPLETED -> Color(0xFF10B981)
        AssistantState.ERROR -> Color(0xFFF87171)
    }

private fun stateText(state: AssistantState): String = state.name.lowercase()
