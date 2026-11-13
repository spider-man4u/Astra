package com.spider.astra.assistant.core

import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.models.AssistantResponse
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * A mock assistant repository for development and testing.
 *
 * It simulates a planning/thinking/executing cycle and always returns an honest
 * simulated result. It never claims to be connected to a real backend, never
 * performs device actions, and never fabricates capabilities beyond text echo/planning.
 */
class MockAssistantRepository(private val scope: CoroutineScope) : AssistantRepository {

    private val _state = MutableStateFlow(AssistantState.IDLE)
    override val stateFlow: StateFlow<AssistantState> = _state.asStateFlow()

    private val messages = ConcurrentHashMap<String, AssistantMessage>()

    private val tasks = ConcurrentHashMap<String, AssistantTask>()

    init {
        // Seed a friendly opening line so the dashboard is not empty.
        addAssistantMessage(
            "I am ASTRA, your personal companion. I am running locally in mock mode. " +
                "No backend is connected yet. Type a command to see the thinking flow.",
            AssistantMessage.AssistantMessageState.READY
        )
    }

    override suspend fun sendCommand(command: String): AssistantResponse {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return AssistantResponse(false, "Please enter a command.", error = "empty_command")
        }

        // Record user message.
        addUserMessage(trimmed)

        // Simulated lifecycle: thinking -> planning -> executing -> completed.
        emitState(AssistantState.THINKING)
        delay(900)
        emitState(AssistantState.PLANNING)
        delay(700)
        emitState(AssistantState.EXECUTING)
        delay(1200)

        // Honest simulated outcome: we acknowledge the command but perform no real action.
        val reply = when {
            trimmed.lowercase().contains("hello") || trimmed.lowercase().contains("hi") ->
                "Hello, Spider. I am ASTRA, running in mock mode. I heard you, but I cannot perform device actions until the real backend is integrated."
            trimmed.lowercase().contains("status") ->
                "Mock status: ASTRA is operational. All backend integrations are pending Phase 5."
            else ->
                "I have received your command: \"$trimmed\". In this milestone ASTRA simulates processing only — no device action was performed. Backend integration comes in Phase 5."
        }

        emitState(AssistantState.COMPLETED)
        delay(400)
        emitState(AssistantState.IDLE)

        addAssistantMessage(reply)

        // Create a completed task record so the activity panel has something to show.
        val task = createTask(trimmed).apply {
            this.status = TaskStatus.COMPLETED
            this.result = reply
            this.updatedAt = java.lang.System.currentTimeMillis()
        }
        updateTask(task)

        return AssistantResponse(true, reply, taskId = task.id)
    }

    override fun messages(): List<AssistantMessage> =
        messages.values.toList().sortedBy { it.timestamp.time }

    override fun addUserMessage(text: String) {
        val id = java.util.UUID.randomUUID().toString()
        messages[id] = AssistantMessage(id = id, text = text, direction = AssistantMessage.Direction.USER)
    }

    override fun addAssistantMessage(
        text: String,
        state: AssistantMessage.AssistantMessageState
    ) {
        val id = java.util.UUID.randomUUID().toString()
        messages[id] = AssistantMessage(
            id = id,
            text = text,
            direction = AssistantMessage.Direction.ASSISTANT,
            state = state
        )
    }

    override fun updateLastAssistantMessage(
        text: String,
        state: AssistantMessage.AssistantMessageState
    ) {
        val last = messages.values.maxByOrNull { it.timestamp.time }
        if (last != null && last.direction == AssistantMessage.Direction.ASSISTANT) {
            messages[last.id] = last.copy(text = text, state = state)
        } else {
            addAssistantMessage(text, state)
        }
    }

    override fun createTask(command: String): AssistantTask {
        val task = AssistantTask(command = command)
        tasks[task.id] = task
        return task
    }

    override fun updateTask(task: AssistantTask) {
        tasks[task.id] = task.copy(updatedAt = java.lang.System.currentTimeMillis())
    }

    override fun getTask(taskId: String): AssistantTask? = tasks[taskId]

    override fun recentTasks(limit: Int): List<AssistantTask> =
        tasks.values.toList().sortedByDescending { it.createdAt }.take(limit)

    private fun emitState(newState: AssistantState) {
        scope.launch(Dispatchers.Main) {
            _state.value = newState
        }
    }
}
