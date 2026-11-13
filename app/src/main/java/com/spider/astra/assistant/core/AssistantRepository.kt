package com.spider.astra.assistant.core

import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.models.AssistantResponse
import com.spider.astra.assistant.state.AssistantTask
import kotlinx.coroutines.flow.StateFlow

/** UI-facing contract for the local, permission-aware assistant task engine. */
interface AssistantRepository {
    val stateFlow: StateFlow<AssistantState>
    val messagesFlow: StateFlow<List<AssistantMessage>>
    val tasksFlow: StateFlow<List<AssistantTask>>

    suspend fun sendCommand(command: String): AssistantResponse
    suspend fun approveTask(taskId: String): Boolean
    suspend fun cancelTask(taskId: String): Boolean
    fun messages(): List<AssistantMessage>
    fun addUserMessage(text: String)
    fun addAssistantMessage(
        text: String,
        state: AssistantMessage.AssistantMessageState = AssistantMessage.AssistantMessageState.READY
    )
    fun updateLastAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState)
}

enum class AssistantState {
    IDLE, LISTENING, THINKING, PLANNING, EXECUTING,
    WAITING_FOR_PERMISSION, COMPLETED, ERROR
}

sealed class SendResult {
    object Success : SendResult()
    data class Failure(val message: String) : SendResult()
}
