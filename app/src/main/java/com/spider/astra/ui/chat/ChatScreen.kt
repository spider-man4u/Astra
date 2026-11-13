package com.spider.astra.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.bridge.BridgeTokenStore
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.ui.components.TaskActionCard
import com.spider.astra.ui.theme.AstraPalette
import com.spider.astra.voice.AstraVoiceController
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    navController: NavHostController,
    repository: AssistantRepository,
    settings: AstraSettings
) {
    val context = LocalContext.current
    val termuxConfigured = remember(context) { BridgeTokenStore(context).hasToken() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by repository.stateFlow.collectAsState()
    val messages by repository.messagesFlow.collectAsState()
    val tasks by repository.tasksFlow.collectAsState()
    val voiceResponsesEnabled by settings.voiceEnabled.collectAsState(initial = false)
    val voiceLanguage by settings.voiceLanguage.collectAsState(initial = "en-IN")
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val voiceController = remember(context) { AstraVoiceController(context) }
    val voiceState by voiceController.state.collectAsState()

    val isBusy = state == AssistantState.THINKING ||
        state == AssistantState.PLANNING || state == AssistantState.EXECUTING
    val pendingApprovals = tasks.filter { it.status == TaskStatus.AWAITING_CONFIRMATION }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.startListening(voiceLanguage)
        } else {
            voiceController.reportError("Microphone access was denied. Enable it in Android Settings to use voice input.")
        }
    }

    DisposableEffect(lifecycleOwner, voiceController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                voiceController.cancelListening()
                voiceController.stopSpeaking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            voiceController.release()
        }
    }

    LaunchedEffect(voiceState.transcript) {
        val recognised = voiceState.transcript?.trim()
        if (!recognised.isNullOrBlank()) {
            voiceController.clearTranscript()
            val response = repository.sendCommand(recognised)
            if (voiceResponsesEnabled && response.message.isNotBlank()) {
                voiceController.speak(response.message, voiceLanguage)
            }
        }
    }

    LaunchedEffect(messages.size, pendingApprovals.size) {
        val total = messages.size + pendingApprovals.size +
            if (pendingApprovals.isNotEmpty()) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    fun send() {
        val clean = command.trim()
        if (clean.isNotEmpty() && !isBusy) {
            command = ""
            scope.launch {
                val response = repository.sendCommand(clean)
                if (voiceResponsesEnabled && response.message.isNotBlank()) {
                    voiceController.speak(response.message, voiceLanguage)
                }
            }
        }
    }

    fun toggleMicrophone() {
        if (voiceState.isListening) {
            voiceController.stopListening()
            return
        }

        voiceController.clearError()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            voiceController.startListening(voiceLanguage)
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        containerColor = AstraPalette.Background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(AstraPalette.Background)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                Surface(
                    color = AstraPalette.Panel,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(if (voiceState.isListening) AstraPalette.Amber else AstraPalette.Green, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (voiceState.isListening) "LISTENING" else if (voiceState.isSpeaking) "SPEAKING" else "LOCAL",
                            color = AstraPalette.Muted,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = AstraPalette.Panel, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Try status, help or note: …", color = AstraPalette.Dim) },
                        singleLine = true,
                        enabled = !isBusy && !voiceState.isListening,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstraPalette.Cyan,
                            unfocusedBorderColor = AstraPalette.Border,
                            focusedTextColor = AstraPalette.Text,
                            unfocusedTextColor = AstraPalette.Text,
                            cursorColor = AstraPalette.Cyan
                        )
                    )
                    Spacer(Modifier.width(5.dp))
                    IconButton(
                        onClick = { toggleMicrophone() },
                        enabled = voiceState.recognitionAvailable,
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                            .background(if (voiceState.isListening) AstraPalette.Amber else AstraPalette.PanelRaised)
                    ) {
                        Text(
                            if (voiceState.isListening) "■" else "🎙",
                            color = if (voiceState.isListening) AstraPalette.BackgroundDeep else AstraPalette.Cyan,
                            fontSize = 19.sp
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    IconButton(
                        onClick = { send() },
                        enabled = command.isNotBlank() && !isBusy && !voiceState.isListening,
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                            .background(if (command.isNotBlank() && !isBusy) AstraPalette.CyanBright else AstraPalette.PanelRaised)
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = if (command.isNotBlank() && !isBusy) AstraPalette.BackgroundDeep else AstraPalette.Dim
                        )
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
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✦", color = AstraPalette.Cyan, fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Tap-to-talk · " + voiceLanguage + " · No background listening",
                            color = AstraPalette.Muted,
                            fontSize = 10.sp
                        )
                    }
                    if (voiceState.isListening || voiceState.partialTranscript.isNotBlank()) {
                        Text(
                            voiceState.partialTranscript.ifBlank { "Listening… speak now" },
                            color = AstraPalette.Cyan,
                            fontSize = 11.sp
                        )
                    } else if (voiceState.isSpeaking) {
                        Text("ASTRA is speaking your reply…", color = AstraPalette.Violet, fontSize = 10.sp)
                    } else if (!voiceState.recognitionAvailable) {
                        Text("No speech recognition service is available. Enable a recognizer in Android Settings.", color = AstraPalette.Amber, fontSize = 10.sp)
                    } else if (voiceState.errorMessage != null) {
                        Text(voiceState.errorMessage.orEmpty(), color = AstraPalette.Amber, fontSize = 10.sp)
                    } else if (voiceResponsesEnabled && !voiceState.ttsReady) {
                        Text("Preparing speech engine…", color = AstraPalette.Dim, fontSize = 10.sp)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingApprovals.isNotEmpty()) {
                    item {
                        Text(
                            "PENDING APPROVALS",
                            color = AstraPalette.Amber,
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(pendingApprovals, key = { "approval-" + it.id }) { task ->
                        TaskActionCard(
                            task = task,
                            onApprove = { scope.launch { repository.approveTask(task.id) } },
                            onCancel = { scope.launch { repository.cancelTask(task.id) } }
                        )
                    }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onSpeak = if (message.direction == AssistantMessage.Direction.ASSISTANT) {
                            { voiceController.speak(message.text, voiceLanguage) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage, onSpeak: (() -> Unit)?) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isUser) "YOU" else "ASTRA CORE",
                        modifier = Modifier.weight(1f),
                        color = if (isUser) AstraPalette.Cyan else AstraPalette.Violet,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (onSpeak != null) {
                        TextButton(onClick = onSpeak, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text("🔊", fontSize = 14.sp)
                        }
                    }
                }
                Text(message.text, color = AstraPalette.Text, fontSize = 13.sp, lineHeight = 19.sp)
                if (message.state == AssistantMessage.AssistantMessageState.FAILED) {
                    Spacer(Modifier.height(4.dp))
                    Text("TRANSMISSION FAILED", color = AstraPalette.Red, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}
