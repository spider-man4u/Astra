package com.spider.astra.assistant.models

import java.util.Date

/**
 * A single message in a conversation with ASTRA.
 *
 * [direction] indicates whether the message came from the user or from ASTRA.
 * [timestamp] is when the message was created.
 */
data class AssistantMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val direction: Direction = Direction.ASSISTANT,
    val timestamp: Date = Date(),
    val state: AssistantMessageState = AssistantMessageState.READY
) {
    enum class Direction {
        USER, ASSISTANT
    }

    enum class AssistantMessageState {
        /** Message is ready to display. */
        READY,
        /** ASTRA is still generating this message (streaming/placeholder). */
        STREAMING,
        /** The message could not be delivered. */
        FAILED
    }
}

/**
 * The structured response ASTRA produces after processing a request.
 *
 * [success] is never "faked": if the underlying action failed, [success] is false
 * and [error] describes what happened.
 */
data class AssistantResponse(
    val success: Boolean,
    val message: String,
    val taskId: String? = null,
    val error: String? = null
)
