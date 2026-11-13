package com.spider.astra.assistant.core

import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.models.AssistantResponse
import com.spider.astra.assistant.state.AssistantTask
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstraction for the ASTRA assistant backend.
 *
 * The first milestone ships with [MockAssistantRepository] so the entire app can be
 * tested without a real backend. A later milestone (Phase 5) replaces this with an
 * implementation that talks to the existing Python ASTRA backend through a local,
 * authenticated HTTP API (or another suitable local IPC mechanism).
 *
 * No API keys or credentials are stored here. Backend connectivity is never faked:
 * if the backend cannot be reached, the repository reports the failure honestly.
 */
interface AssistantRepository {
    /** The current conversational state (idle, listening, thinking, …). */
    val stateFlow: Flow<AssistantState>

    /** Send a text command and receive a response. */
    suspend fun sendCommand(command: String): AssistantResponse

    /** The conversation history. */
    fun messages(): List<AssistantMessage>

    /** Add a user message to history (used before awaiting an assistant reply). */
    fun addUserMessage(text: String)

    /** Add an assistant message to history. */
    fun addAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState = AssistantMessage.AssistantMessageState.READY)

    /** Replace the last assistant message (e.g. to finalize a streaming response). */
    fun updateLastAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState)

    /** Task management. */
    fun createTask(command: String): AssistantTask
    fun updateTask(task: AssistantTask)
    fun getTask(taskId: String): AssistantTask?
    fun recentTasks(limit: Int = 10): List<AssistantTask>
}

/**
 * The high-level conversational/execution state exposed to the UI.
 */
enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    PLANNING,
    EXECUTING,
    WAITING_FOR_PERMISSION,
    COMPLETED,
    ERROR
}

/**
 * Result of attempting to send a command.
 */
sealed class SendResult {
    object Success : SendResult()
    data class Failure(val message: String) : SendResult()
}
