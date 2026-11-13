package com.spider.astra.assistant.core

import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.models.AssistantResponse
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Local demo implementation. It never performs device actions or claims a backend is connected. */
class MockAssistantRepository : AssistantRepository {
    private val _state = MutableStateFlow(AssistantState.IDLE)
    override val stateFlow: StateFlow<AssistantState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    override val messagesFlow: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    private val _tasks = MutableStateFlow<List<AssistantTask>>(emptyList())
    override val tasksFlow: StateFlow<List<AssistantTask>> = _tasks.asStateFlow()

    private val commandMutex = Mutex()
    private val lock = Any()
    private val taskStore = linkedMapOf<String, AssistantTask>()

    init {
        addAssistantMessage(
            "I am ASTRA, running in local demo mode. I can simulate a command flow here, " +
                "but no device actions or real AI backend are connected yet."
        )
    }

    override suspend fun sendCommand(command: String): AssistantResponse {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return AssistantResponse(false, "Please enter a command.", error = "empty_command")
        }

        return commandMutex.withLock {
            addUserMessage(trimmed)
            var task = createTask(trimmed)
            try {
                _state.value = AssistantState.THINKING
                delay(450)

                _state.value = AssistantState.PLANNING
                task = task.copy(status = TaskStatus.PLANNING)
                updateTask(task)
                delay(450)

                _state.value = AssistantState.EXECUTING
                task = task.copy(status = TaskStatus.EXECUTING)
                updateTask(task)
                delay(650)

                val reply = when {
                    trimmed.contains("hello", ignoreCase = true) ||
                        trimmed.contains("hi", ignoreCase = true) ->
                        "Hello, Spider. ASTRA is here. This is a local demo, so I have not performed any device action."
                    trimmed.contains("status", ignoreCase = true) ->
                        "ASTRA status: the Android UI and local demo flow are running. The real AI backend and device integrations are still pending."
                    else ->
                        "I received: \"$trimmed\". I simulated the thinking and planning steps, but did not execute a real-world action."
                }

                addAssistantMessage(reply)
                task = task.copy(status = TaskStatus.COMPLETED, result = reply, error = null)
                updateTask(task)
                _state.value = AssistantState.COMPLETED
                delay(250)
                AssistantResponse(success = true, message = reply, taskId = task.id)
            } catch (cancelled: CancellationException) {
                task = task.copy(status = TaskStatus.CANCELLED, error = "Command cancelled.")
                updateTask(task)
                throw cancelled
            } catch (failure: Exception) {
                val detail = failure.message ?: "Unexpected demo error."
                val reply = "I couldn't finish that demo command: $detail"
                addAssistantMessage(reply, AssistantMessage.AssistantMessageState.FAILED)
                task = task.copy(status = TaskStatus.FAILED, error = detail)
                updateTask(task)
                _state.value = AssistantState.ERROR
                AssistantResponse(false, reply, taskId = task.id, error = detail)
            } finally {
                _state.value = AssistantState.IDLE
            }
        }
    }

    override fun messages(): List<AssistantMessage> = _messages.value

    override fun addUserMessage(text: String) {
        val message = AssistantMessage(text = text, direction = AssistantMessage.Direction.USER)
        synchronized(lock) { _messages.value = _messages.value + message }
    }

    override fun addAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState) {
        val message = AssistantMessage(
            text = text,
            direction = AssistantMessage.Direction.ASSISTANT,
            state = state
        )
        synchronized(lock) { _messages.value = _messages.value + message }
    }

    override fun updateLastAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState) {
        synchronized(lock) {
            val index = _messages.value.indexOfLast {
                it.direction == AssistantMessage.Direction.ASSISTANT
            }
            if (index < 0) {
                _messages.value = _messages.value + AssistantMessage(
                    text = text,
                    direction = AssistantMessage.Direction.ASSISTANT,
                    state = state
                )
            } else {
                _messages.value = _messages.value.toMutableList().also {
                    it[index] = it[index].copy(text = text, state = state)
                }
            }
        }
    }

    override fun createTask(command: String): AssistantTask {
        val task = AssistantTask(command = command)
        synchronized(lock) {
            taskStore[task.id] = task
            publishTasksLocked()
        }
        return task
    }

    override fun updateTask(task: AssistantTask) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        synchronized(lock) {
            taskStore[updated.id] = updated
            publishTasksLocked()
        }
    }

    override fun getTask(taskId: String): AssistantTask? = synchronized(lock) {
        taskStore[taskId]
    }

    override fun recentTasks(limit: Int): List<AssistantTask> = synchronized(lock) {
        taskStore.values.sortedByDescending { it.createdAt }.take(limit.coerceAtLeast(0))
    }

    private fun publishTasksLocked() {
        _tasks.value = taskStore.values.sortedByDescending { it.createdAt }
    }
}
