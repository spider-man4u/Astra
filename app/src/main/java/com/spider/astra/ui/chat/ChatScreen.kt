package com.spider.astra.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.ui.theme.AstraPalette
import kotlinx.coroutines.launch

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
        val clean = command.trim()
        if (clean.isNotEmpty() && !isBusy) {
            command = ""
            scope.launch { repository.sendCommand(clean) }
        }
    }

    Scaffold(
        containerColor = AstraPalette.Background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(AstraPalette.Background).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(AstraPalette.Panel)
                        .border(1.dp, AstraPalette.Border, CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AstraPalette.Cyan)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("COMMS CHANNEL", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.8.sp)
                    Text("ASTRA", color = AstraPalette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = AstraPalette.Panel, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(if (isBusy) AstraPalette.Violet else AstraPalette.Green, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(if (isBusy) state.name else "LOCAL", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = AstraPalette.Panel, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Transmit a message…", color = AstraPalette.Dim) },
                        singleLine = true,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstraPalette.Cyan,
                            unfocusedBorderColor = AstraPalette.Border,
                            focusedTextColor = AstraPalette.Text,
                            unfocusedTextColor = AstraPalette.Text,
                            cursorColor = AstraPalette.Cyan
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { send() },
                        enabled = command.isNotBlank() && !isBusy,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(if (command.isNotBlank() && !isBusy) AstraPalette.CyanBright else AstraPalette.PanelRaised)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = if (command.isNotBlank() && !isBusy) AstraPalette.BackgroundDeep else AstraPalette.Dim)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = AstraPalette.PanelSoft,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = AstraPalette.Cyan, fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Local simulation only · No device actions are executed",
                        color = AstraPalette.Muted,
                        fontSize = 10.sp
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            color = if (isUser) Color(0xFF123A55) else AstraPalette.Panel,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 5.dp,
                bottomEnd = if (isUser) 5.dp else 20.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) AstraPalette.Cyan.copy(alpha = 0.30f) else AstraPalette.Border
            )
        ) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                Text(
                    if (isUser) "YOU" else "ASTRA CORE",
                    color = if (isUser) AstraPalette.Cyan else AstraPalette.Violet,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(message.text, color = AstraPalette.Text, fontSize = 13.sp, lineHeight = 19.sp)
                if (message.state == AssistantMessage.AssistantMessageState.FAILED) {
                    Spacer(Modifier.height(4.dp))
                    Text("TRANSMISSION FAILED", color = AstraPalette.Red, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}
