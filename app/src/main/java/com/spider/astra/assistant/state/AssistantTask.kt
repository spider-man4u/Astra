package com.spider.astra.assistant.state

enum class TaskStatus {
    IDLE, PENDING, AWAITING_CONFIRMATION, PLANNING, EXECUTING,
    WAITING_FOR_PERMISSION, COMPLETED, FAILED, CANCELLED
}

/** Durable task description. Command text is never interpreted as executable code. */
data class AssistantTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val result: String? = null,
    val error: String? = null,
    val actionKey: String? = null,
    val payload: String? = null,
    val requiresConfirmation: Boolean = false,
    val confirmed: Boolean = false
)
