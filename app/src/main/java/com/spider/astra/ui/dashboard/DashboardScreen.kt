package com.spider.astra.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.spider.astra.assistant.core.AssistantRepository
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.settings.AstraSettings
import com.spider.astra.navigation.Screens
import com.spider.astra.ui.components.AstraOrb
import com.spider.astra.ui.components.TaskActionCard
import com.spider.astra.ui.theme.AstraPalette
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(navController: NavHostController, repository: AssistantRepository, settings: AstraSettings) {
    val state by repository.stateFlow.collectAsState()
    val userName by settings.userName.collectAsState(initial = null)
    val tasks by repository.tasksFlow.collectAsState()
    var command by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteDraft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val isBusy = state == AssistantState.THINKING ||
        state == AssistantState.PLANNING || state == AssistantState.EXECUTING
    val awaitingCount = tasks.count { it.status == TaskStatus.AWAITING_CONFIRMATION }

    fun submitCommand(raw: String) {
        val clean = raw.trim()
        if (clean.isNotEmpty() && !isBusy) scope.launch { repository.sendCommand(clean) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(AstraPalette.Background, Color(0xFF071426), AstraPalette.BackgroundDeep))
        )
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.27f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x3321D9F4), Color(0x10258BFF), Color.Transparent),
                    center = center,
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = center
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(AstraPalette.Cyan, AstraPalette.Blue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = AstraPalette.BackgroundDeep, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("ASTRA", color = AstraPalette.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("PERSONAL INTELLIGENCE", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                    Surface(
                        color = AstraPalette.Panel.copy(alpha = 0.88f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(AstraPalette.Green, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text("TASK ENGINE", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { navController.navigate(Screens.SETTINGS) },
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(AstraPalette.Panel)
                            .border(1.dp, AstraPalette.Border, CircleShape)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = AstraPalette.Cyan)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "GOOD TO SEE YOU, " + (userName?.takeIf { it.isNotBlank() }?.uppercase() ?: "SPIDER"),
                        color = AstraPalette.Cyan,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("What shall we\nmake happen?", color = AstraPalette.Text, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.7).sp)
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(344.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF102A43), Color(0xFF0A1A2D), Color(0xFF091323))))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Color(0x6654E8FF), Color(0x222C5F8C), Color(0x443F68A1))),
                            RoundedCornerShape(30.dp)
                        )
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.47f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0x2224DDF5), Color(0x083B82F6), Color.Transparent),
                                center = center,
                                radius = size.width * 0.54f
                            ),
                            radius = size.width * 0.54f,
                            center = center
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SYSTEM CORE  /  ASTRA-01", color = AstraPalette.Muted, fontSize = 9.sp, letterSpacing = 2.3.sp)
                        AstraOrb(state = state, modifier = Modifier.size(224.dp), size = 224.dp, glowEnabled = true)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = stateColor(state).copy(alpha = 0.12f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, stateColor(state).copy(alpha = 0.35f))
                            ) {
                                Row(Modifier.padding(horizontal = 13.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).background(stateColor(state), CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stateLabel(state).uppercase(), color = stateColor(state), fontSize = 10.sp, letterSpacing = 1.4.sp)
                                }
                            }
                            Text(if (isBusy) "Processing your request" else "Awaiting your command", color = AstraPalette.Muted, fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("COMMAND LINK", color = AstraPalette.Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                        Text("ALLOWLISTED ACTIONS", color = AstraPalette.Dim, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .background(AstraPalette.Panel)
                            .border(1.dp, AstraPalette.Border, RoundedCornerShape(20.dp))
                            .padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("›", color = AstraPalette.Cyan, fontSize = 28.sp, fontWeight = FontWeight.Light)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Try “status” or “help”…", color = AstraPalette.Dim) },
                            singleLine = true,
                            enabled = !isBusy,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                submitCommand(command)
                                command = ""
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedTextColor = AstraPalette.Text,
                                unfocusedTextColor = AstraPalette.Text,
                                cursorColor = AstraPalette.Cyan
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { submitCommand(command); command = "" },
                            enabled = command.isNotBlank() && !isBusy,
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(
                                    if (command.isNotBlank() && !isBusy)
                                        Brush.linearGradient(listOf(AstraPalette.Cyan, AstraPalette.Blue))
                                    else Brush.linearGradient(listOf(AstraPalette.PanelRaised, AstraPalette.PanelRaised))
                                )
                        ) {
                            Text("↑", color = if (command.isNotBlank() && !isBusy) AstraPalette.BackgroundDeep else AstraPalette.Dim, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("QUICK ACCESS", color = AstraPalette.Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                        Text("SAFE ACTIONS", color = AstraPalette.Dim, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionTile("System scan", "Read-only status", "⌁", Modifier.weight(1f)) { submitCommand("Show ASTRA status") }
                        ActionTile("Create note", "Needs your approval", "✎", Modifier.weight(1f)) {
                            noteDraft = ""
                            showNoteDialog = true
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionTile("Available actions", "Show safe commands", "⌘", Modifier.weight(1f)) { submitCommand("help") }
                        ActionTile("Conversations", "Open assistant chat", "✧", Modifier.weight(1f)) { navController.navigate(Screens.CHAT) }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("ACTIVITY LOG", color = AstraPalette.Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (awaitingCount > 0) "$awaitingCount task(s) need your approval" else "Persistent task history",
                            color = if (awaitingCount > 0) AstraPalette.Amber else AstraPalette.Dim,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = { navController.navigate(Screens.CHAT) }) {
                        Text("VIEW ALL  ↗", color = AstraPalette.Cyan, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AstraPalette.Panel.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border.copy(alpha = 0.8f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⌘", color = AstraPalette.Cyan, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("No operations yet", color = AstraPalette.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Your completed requests will appear here.", color = AstraPalette.Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                items(tasks.take(5), key = { it.id }) { task ->
                    TaskActionCard(
                        task = task,
                        onApprove = { scope.launch { repository.approveTask(task.id) } },
                        onCancel = { scope.launch { repository.cancelTask(task.id) } }
                    )
                }
            }

            item {
                Text(
                    "PHASE 3  ·  PERSISTENT TASKS  ·  SAFE ACTION REGISTRY",
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = AstraPalette.Dim,
                    fontSize = 9.sp,
                    letterSpacing = 1.1.sp
                )
            }
        }

        if (showNoteDialog) {
            AlertDialog(
                onDismissRequest = { showNoteDialog = false },
                containerColor = AstraPalette.Panel,
                title = { Text("Create a private note", color = AstraPalette.Text) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "ASTRA will save this note only after you explicitly confirm its task card.",
                            color = AstraPalette.Muted,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { if (it.length <= 500) noteDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Write your note…") },
                            minLines = 2,
                            maxLines = 4
                        )
                        Text(noteDraft.length.toString() + "/500", color = AstraPalette.Dim, fontSize = 10.sp)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val note = noteDraft.trim()
                            showNoteDialog = false
                            if (note.isNotEmpty()) scope.launch { repository.sendCommand("note: " + note) }
                        },
                        enabled = noteDraft.trim().isNotEmpty()
                    ) { Text("CREATE REQUEST", color = AstraPalette.Cyan) }
                },
                dismissButton = {
                    TextButton(onClick = { showNoteDialog = false }) { Text("CANCEL", color = AstraPalette.Muted) }
                }
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    glyph: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        color = AstraPalette.Panel.copy(alpha = 0.94f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)
    ) {
        Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(glyph, color = AstraPalette.Cyan, fontSize = 21.sp, fontWeight = FontWeight.Light)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = AstraPalette.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = AstraPalette.Muted, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

private fun stateLabel(state: AssistantState): String =
    state.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun stateColor(state: AssistantState): Color = when (state) {
    AssistantState.IDLE -> AstraPalette.Cyan
    AssistantState.LISTENING -> AstraPalette.Blue
    AssistantState.THINKING, AssistantState.PLANNING -> AstraPalette.Violet
    AssistantState.EXECUTING, AssistantState.WAITING_FOR_PERMISSION -> AstraPalette.Amber
    AssistantState.COMPLETED -> AstraPalette.Green
    AssistantState.ERROR -> AstraPalette.Red
}
