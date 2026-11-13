package com.spider.astra.assistant.core

import android.content.Context
import com.spider.astra.assistant.models.AssistantMessage
import com.spider.astra.assistant.models.AssistantResponse
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.bridge.BridgeTokenStore
import com.spider.astra.bridge.TermuxBridgeClient
import com.spider.astra.data.tasks.AssistantTaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Hybrid ASTRA repository.
 *
 * If an encrypted Termux token is configured, commands are sent to the real
 * ASTRA v2 handler. If configured but offline, they fail visibly—never fall
 * back to the mock engine and risk silently executing a different action.
 * Without a token, the local Phase 3 allowlisted demo remains available.
 */
class AstraAssistantRepository(context: Context) : AssistantRepository {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskStore = AssistantTaskStore.create(appContext)
    private val orchestrator = TaskOrchestrator(appContext)
    private val bridgeTokenStore = BridgeTokenStore(appContext)
    private val bridgeClient = TermuxBridgeClient(bridgeTokenStore)

    private val _state = MutableStateFlow(AssistantState.IDLE)
    override val stateFlow: StateFlow<AssistantState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    override val messagesFlow: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    override val tasksFlow: StateFlow<List<AssistantTask>> =
        taskStore.tasksFlow.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val commandMutex = Mutex()
    private val messageLock = Any()

    init {
        addAssistantMessage(
            "ASTRA is ready. Configure the Termux Core link to use your Python assistant, or leave it disconnected to use the local safe-action demo."
        )
    }

    override suspend fun sendCommand(command: String): AssistantResponse {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return AssistantResponse(false, "Please enter a command.", error = "empty_command")
        }

        return commandMutex.withLock {
            addUserMessage(trimmed)

            val remoteConfigured = bridgeClient.isConfigured()
            if (remoteConfigured) {
                _state.value = AssistantState.THINKING
                val remote = bridgeClient.sendCommand(trimmed)
                addAssistantMessage(
                    remote.reply,
                    if (remote.success) AssistantMessage.AssistantMessageState.READY
                    else AssistantMessage.AssistantMessageState.FAILED
                )
                _state.value = if (remote.success) AssistantState.COMPLETED else AssistantState.ERROR
                return@withLock AssistantResponse(
                    success = remote.success,
                    message = remote.reply,
                    error = remote.error
                )
            }

            _state.value = AssistantState.PLANNING
            if (trimmed.equals("help", ignoreCase = true) ||
                trimmed.equals("available actions", ignoreCase = true)
            ) {
                val help = "Available actions right now:\n• System status — read-only and runs automatically.\n• note: your text — asks for confirmation, then saves privately inside ASTRA.\n\nArbitrary shell commands and device-control actions are not enabled."
                addAssistantMessage(help)
                _state.value = AssistantState.IDLE
                return@withLock AssistantResponse(true, help)
            }

            val planned = ActionPlanner.plan(trimmed)
            if (planned == null) {
                val message = "I don't have that action enabled yet. Nothing was executed. Try “status”, “system scan”, “help”, or “note: your text”."
                addAssistantMessage(message)
                _state.value = AssistantState.IDLE
                return@withLock AssistantResponse(false, message, error = "unsupported_action")
            }

            val task = AssistantTask(
                command = trimmed,
                status = if (planned.requiresConfirmation) TaskStatus.AWAITING_CONFIRMATION else TaskStatus.PENDING,
                actionKey = planned.actionKey,
                payload = planned.payload,
                requiresConfirmation = planned.requiresConfirmation
            )
            taskStore.upsertTask(task)

            val message = if (planned.requiresConfirmation) {
                "This will save the note only inside ASTRA's private storage. Review the pending task and tap CONFIRM before it is saved."
            } else {
                "Read-only system status check queued. The result will appear in the activity log."
            }

            if (!planned.requiresConfirmation) orchestrator.enqueue(task)
            addAssistantMessage(message)
            _state.value = AssistantState.IDLE
            AssistantResponse(true, message, taskId = task.id)
        }
    }

    override suspend fun approveTask(taskId: String): Boolean {
        val approved = orchestrator.approveTask(taskId)
        if (approved) addAssistantMessage("Task approved. ASTRA queued the allowlisted action.")
        return approved
    }

    override suspend fun cancelTask(taskId: String): Boolean {
        val cancelled = orchestrator.cancelTask(taskId)
        if (cancelled) addAssistantMessage("Task cancelled. No further action will be taken.")
        return cancelled
    }

    override fun messages(): List<AssistantMessage> = _messages.value

    override fun addUserMessage(text: String) {
        val message = AssistantMessage(text = text, direction = AssistantMessage.Direction.USER)
        synchronized(messageLock) { _messages.value = _messages.value + message }
    }

    override fun addAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState) {
        val message = AssistantMessage(
            text = text,
            direction = AssistantMessage.Direction.ASSISTANT,
            state = state
        )
        synchronized(messageLock) { _messages.value = _messages.value + message }
    }

    override fun updateLastAssistantMessage(text: String, state: AssistantMessage.AssistantMessageState) {
        synchronized(messageLock) {
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
}
