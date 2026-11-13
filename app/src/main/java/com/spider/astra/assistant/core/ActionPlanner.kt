package com.spider.astra.assistant.core

import android.content.Context
import android.os.Build
import com.spider.astra.data.tasks.AssistantTaskStore

data class PlannedAction(
    val actionKey: String,
    val payload: String? = null,
    val requiresConfirmation: Boolean = false
)

/** Converts a few explicit request forms into a tiny, auditable allowlist. */
object ActionPlanner {
    const val STATUS_CHECK = "STATUS_CHECK"
    const val SAVE_NOTE = "SAVE_NOTE"

    private val allowedActions = setOf(STATUS_CHECK, SAVE_NOTE)
    private val notePattern = Regex(
        "^(?:note|save note)\\s*:\\s*(.+)$",
        RegexOption.IGNORE_CASE
    )

    fun isAllowed(actionKey: String?): Boolean =
        actionKey != null && actionKey in allowedActions

    fun plan(command: String): PlannedAction? {
        val clean = command.trim()
        if (clean.isEmpty()) return null

        val match = notePattern.matchEntire(clean)
        if (match != null) {
            val note = match.groupValues[1].trim()
            if (note.isEmpty() || note.length > 500) return null
            return PlannedAction(SAVE_NOTE, payload = note, requiresConfirmation = true)
        }

        val normalized = clean.lowercase()
        val statusRequest = normalized == "status" ||
            normalized.contains("status") ||
            normalized == "system scan" ||
            normalized == "check system"

        return if (statusRequest) PlannedAction(STATUS_CHECK) else null
    }

    suspend fun execute(
        store: AssistantTaskStore,
        actionKey: String,
        payload: String?
    ): String {
        require(isAllowed(actionKey)) { "This action is not on ASTRA's allowlist." }
        return when (actionKey) {
            STATUS_CHECK -> {
                val androidVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()
                "Read-only status check completed. ASTRA task engine is active on Android $androidVersion. No settings were changed."
            }
            SAVE_NOTE -> {
                val note = payload?.trim().orEmpty()
                require(note.isNotEmpty() && note.length <= 500) { "The note is empty or too long." }
                store.saveNote(note)
                "Note saved inside ASTRA's private app storage."
            }
            else -> error("Unsupported action.")
        }
    }
}
