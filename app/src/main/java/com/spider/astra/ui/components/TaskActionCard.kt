package com.spider.astra.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.ui.theme.AstraPalette

@Composable
fun TaskActionCard(task: AssistantTask, onApprove: () -> Unit, onCancel: () -> Unit) {
    val awaiting = task.status == TaskStatus.AWAITING_CONFIRMATION
    val active = task.status == TaskStatus.PENDING ||
        task.status == TaskStatus.PLANNING ||
        task.status == TaskStatus.EXECUTING

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AstraPalette.Panel,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstraPalette.Border)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when (task.status) {
                        TaskStatus.COMPLETED -> AstraPalette.Green.copy(alpha = 0.14f)
                        TaskStatus.FAILED -> AstraPalette.Red.copy(alpha = 0.14f)
                        TaskStatus.AWAITING_CONFIRMATION -> AstraPalette.Amber.copy(alpha = 0.14f)
                        else -> AstraPalette.Cyan.copy(alpha = 0.12f)
                    },
                    shape = CircleShape
                ) {
                    Text(
                        task.status.name.replace('_', ' '),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = when (task.status) {
                            TaskStatus.COMPLETED -> AstraPalette.Green
                            TaskStatus.FAILED -> AstraPalette.Red
                            TaskStatus.AWAITING_CONFIRMATION -> AstraPalette.Amber
                            else -> AstraPalette.Cyan
                        },
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(task.actionKey?.replace('_', ' ') ?: "LOCAL TASK", color = AstraPalette.Dim, fontSize = 8.sp, letterSpacing = 1.sp)
            }
            Text(task.command, color = AstraPalette.Text, fontSize = 12.sp)
            val detail = task.error ?: task.result ?: task.payload ?: when {
                awaiting -> "Waiting for your approval. Nothing has been saved yet."
                active -> "Queued with Android WorkManager."
                else -> "No extra details."
            }
            Text(detail, color = AstraPalette.Muted, fontSize = 10.sp, lineHeight = 15.sp)

            if (awaiting) {
                Text(
                    "This task requests permission to save a note in ASTRA's private storage.",
                    color = AstraPalette.Amber,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstraPalette.CyanBright,
                            contentColor = AstraPalette.BackgroundDeep
                        )
                    ) { Text("CONFIRM", fontSize = 9.sp, letterSpacing = 1.sp) }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("DECLINE", fontSize = 9.sp, letterSpacing = 1.sp) }
                }
            } else if (active) {
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                    Text("CANCEL TASK", color = AstraPalette.Red, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}
