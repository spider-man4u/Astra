package com.spider.astra.assistant.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spider.astra.assistant.state.TaskStatus
import com.spider.astra.data.tasks.AssistantTaskStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Executes one persisted, allowlisted task. No command text is passed to a shell. */
class AssistantTaskWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val store = AssistantTaskStore.create(applicationContext)
        val task = store.getTask(taskId) ?: return Result.failure()

        if (task.status == TaskStatus.CANCELLED) return Result.failure()
        if (!ActionPlanner.isAllowed(task.actionKey)) {
            store.upsertTask(task.copy(status = TaskStatus.FAILED, error = "Blocked: action is not on ASTRA's allowlist."))
            return Result.failure()
        }
        if (task.requiresConfirmation && !task.confirmed) {
            store.upsertTask(task.copy(
                status = TaskStatus.AWAITING_CONFIRMATION,
                error = "Explicit user confirmation is required."
            ))
            return Result.failure()
        }

        store.upsertTask(task.copy(status = TaskStatus.EXECUTING, error = null))

        return try {
            val result = withTimeout(TASK_TIMEOUT_MS) {
                ActionPlanner.execute(
                    store = store,
                    actionKey = task.actionKey.orEmpty(),
                    payload = task.payload
                )
            }
            val latest = store.getTask(taskId)
            if (latest?.status == TaskStatus.CANCELLED) {
                Result.failure()
            } else {
                store.upsertTask(task.copy(status = TaskStatus.COMPLETED, result = result, error = null))
                Result.success()
            }
        } catch (timeout: TimeoutCancellationException) {
            store.upsertTask(task.copy(
                status = TaskStatus.FAILED,
                error = "The task exceeded its " + (TASK_TIMEOUT_MS / 1000) + "-second execution limit."
            ))
            Result.failure()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                val latest = store.getTask(taskId)
                if (latest?.status != TaskStatus.COMPLETED) {
                    store.upsertTask(task.copy(status = TaskStatus.CANCELLED, error = "Task cancelled."))
                }
            }
            throw cancelled
        } catch (failure: Exception) {
            val detail = failure.message?.take(240) ?: "Unexpected task error."
            store.upsertTask(task.copy(status = TaskStatus.FAILED, error = detail))
            Result.failure()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val TASK_TAG = "astra_background_task"
        private const val TASK_TIMEOUT_MS = 15_000L
    }
}
