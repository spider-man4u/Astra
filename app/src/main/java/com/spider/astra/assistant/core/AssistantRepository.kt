package com.spider.astra.assistant.core

import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.models.AssistantResponse
import com.spider.astra.assistant.state.AssistantTask
import kotlinx.coroutines.flow.StateFlow

/** UI-facing contract. Phase 1 uses a mock implementation only. */
interface AssistantRepository {
    val stateFlow: StateFlow<AssistantState>
    val messagesFlow: StateFlow<List<AssistantMessage>>
    val tasksFlow: StateFlow<List<AssistantTask>>

    suspend fun sendCommand(command: String): AssistantResponse
    fun messages(): List<AssistantMessage>
    fun addUserMessage(text: String)
    fun addAssistantMessage(
        text: String,
        state: AssistantMessage.AssistantMessageState = AssistantMessage.AssistantMessageState.READY
    )
    fun updateLastAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState)
    fun createTask(command: String): AssistantTask
    fun updateTask(task: AssistantTask)
    fun getTask(taskId: String): AssistantTask?
    fun recentTasks(limit: Int = 10): List<AssistantTask>
}

enum class AssistantState {
    IDLE, LISTENING, THINKING, PLANNING, EXECUTING,
    WAITING_FOR_PERMISSION, COMPLETED, ERROR
}

/** Honest result returned by a command attempt. */
sealed class SendResult {
    object Success : SendResult()
    data class Failure(val message: String) : SendResult()
}
