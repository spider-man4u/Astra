package com.spider.astra.assistant.state

/**
 * The lifecycle states of a task handled by ASTRA.
 */
enum class TaskStatus {
    IDLE,
    PENDING,
    PLANNING,
    EXECUTING,
    WAITING_FOR_PERMISSION,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * A lightweight, persistent task record.
 */
data class AssistantTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val command: String,
    var status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = java.lang.System.currentTimeMillis(),
    var updatedAt: Long = java.lang.System.currentTimeMillis(),
    var result: String? = null,
    var error: String? = null
)
