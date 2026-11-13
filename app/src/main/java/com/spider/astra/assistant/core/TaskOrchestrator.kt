package com.spider.astra.assistant.core

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.spider.astra.assistant.state.AssistantTask
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.tasks.AssistantTaskStore
import java.util.concurrent.TimeUnit

/** Queue coordinator. Only registered actions can be scheduled. */
class TaskOrchestrator(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val store = AssistantTaskStore.create(context.applicationContext)

    fun enqueue(task: AssistantTask) {
        require(ActionPlanner.isAllowed(task.actionKey)) {
            "Refusing to enqueue an action outside ASTRA's allowlist."
        }
        require(!task.requiresConfirmation || task.confirmed) {
            "This task requires explicit confirmation before enqueueing."
        }

        val request = OneTimeWorkRequestBuilder<AssistantTaskWorker>()
            .setInputData(workDataOf(AssistantTaskWorker.KEY_TASK_ID to task.id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
            .addTag(AssistantTaskWorker.TASK_TAG)
            .addTag("astra-task-" + task.id)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_PREFIX + task.id,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun approveTask(taskId: String): Boolean {
        val task = store.getTask(taskId) ?: return false
        if (task.status != TaskStatus.AWAITING_CONFIRMATION ||
            !task.requiresConfirmation ||
            !ActionPlanner.isAllowed(task.actionKey)
        ) return false

        val approved = task.copy(
            status = TaskStatus.PENDING,
            confirmed = true,
            error = null,
            result = "Approved by you; queued for execution."
        )
        store.upsertTask(approved)
        enqueue(approved)
        return true
    }

    suspend fun cancelTask(taskId: String): Boolean {
        val task = store.getTask(taskId) ?: return false
        if (task.status == TaskStatus.COMPLETED ||
            task.status == TaskStatus.FAILED ||
            task.status == TaskStatus.CANCELLED
        ) return false

        workManager.cancelUniqueWork(UNIQUE_PREFIX + taskId)
        store.upsertTask(task.copy(status = TaskStatus.CANCELLED, error = "Cancelled by you."))
        return true
    }

    companion object {
        private const val UNIQUE_PREFIX = "astra-task-"
    }
}
