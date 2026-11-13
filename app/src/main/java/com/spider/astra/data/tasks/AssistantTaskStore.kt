package com.spider.astra.data.tasks

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.astraTaskDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "astra.task_store")

private object TaskStoreKeys {
    val TASKS_JSON = stringPreferencesKey("tasks_json")
    val NOTES_JSON = stringPreferencesKey("notes_json")
}

/** Private app-local task and note storage, bounded to the latest 100 records. */
class AssistantTaskStore private constructor(
    private val dataStore: DataStore<Preferences>
) {
    val tasksFlow: Flow<List<AssistantTask>> = dataStore.data.map { prefs ->
        decodeTasks(prefs[TaskStoreKeys.TASKS_JSON])
    }

    val notesFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        decodeNotes(prefs[TaskStoreKeys.NOTES_JSON])
    }

    suspend fun getTask(taskId: String): AssistantTask? =
        tasksFlow.first().firstOrNull { it.id == taskId }

    suspend fun upsertTask(task: AssistantTask) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        dataStore.edit { prefs ->
            val current = decodeTasks(prefs[TaskStoreKeys.TASKS_JSON])
                .associateBy { it.id }.toMutableMap()
            current[updated.id] = updated
            prefs[TaskStoreKeys.TASKS_JSON] = encodeTasks(
                current.values.sortedByDescending { it.createdAt }.take(MAX_TASKS)
            )
        }
    }

    suspend fun saveNote(note: String) {
        val clean = note.trim()
        require(clean.isNotEmpty()) { "A note cannot be empty." }
        require(clean.length <= MAX_NOTE_LENGTH) {
            "A note can contain at most " + MAX_NOTE_LENGTH + " characters."
        }
        dataStore.edit { prefs ->
            val notes = decodeNotes(prefs[TaskStoreKeys.NOTES_JSON]).toMutableList()
            notes.add(0, clean)
            prefs[TaskStoreKeys.NOTES_JSON] = encodeNotes(notes.take(MAX_NOTES))
        }
    }

    private fun decodeTasks(raw: String?): List<AssistantTask> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val status = runCatching {
                        TaskStatus.valueOf(item.optString("status", TaskStatus.PENDING.name))
                    }.getOrDefault(TaskStatus.FAILED)
                    add(
                        AssistantTask(
                            id = item.optString("id"),
                            command = item.optString("command"),
                            status = status,
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                            result = item.nullableString("result"),
                            error = item.nullableString("error"),
                            actionKey = item.nullableString("actionKey"),
                            payload = item.nullableString("payload"),
                            requiresConfirmation = item.optBoolean("requiresConfirmation", false),
                            confirmed = item.optBoolean("confirmed", false)
                        )
                    )
                }
            }.filter { it.id.isNotBlank() && it.command.isNotBlank() }
                .sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    private fun decodeNotes(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }.take(MAX_NOTES)
        }.getOrDefault(emptyList())
    }

    private fun encodeTasks(tasks: List<AssistantTask>): String {
        val array = JSONArray()
        tasks.forEach { task ->
            val item = JSONObject()
                .put("id", task.id)
                .put("command", task.command)
                .put("status", task.status.name)
                .put("createdAt", task.createdAt)
                .put("updatedAt", task.updatedAt)
                .put("requiresConfirmation", task.requiresConfirmation)
                .put("confirmed", task.confirmed)
            item.put("result", task.result ?: JSONObject.NULL)
            item.put("error", task.error ?: JSONObject.NULL)
            item.put("actionKey", task.actionKey ?: JSONObject.NULL)
            item.put("payload", task.payload ?: JSONObject.NULL)
            array.put(item)
        }
        return array.toString()
    }

    private fun encodeNotes(notes: List<String>): String {
        val array = JSONArray()
        notes.forEach { array.put(it) }
        return array.toString()
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    companion object {
        private const val MAX_TASKS = 100
        private const val MAX_NOTES = 100
        private const val MAX_NOTE_LENGTH = 500

        fun create(context: Context): AssistantTaskStore =
            AssistantTaskStore(context.applicationContext.astraTaskDataStore)
    }
}
