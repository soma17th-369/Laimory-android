package com.soma369.laimory.push

data class DraftCompletionSignal(
    val taskId: String,
    val status: DraftCompletionStatus,
)

enum class DraftCompletionStatus {
    SUCCESS,
    FAILED,
}

object DraftCompletionSignalParser {
    fun parse(data: Map<String, String>): DraftCompletionSignal? =
        parse(
            taskId = data[TASK_ID_KEY],
            status = data[STATUS_KEY],
        )

    fun parse(
        taskId: String?,
        status: String?,
    ): DraftCompletionSignal? {
        val validTaskId = taskId?.takeIf(String::isNotBlank) ?: return null
        val validStatus =
            DraftCompletionStatus.entries.firstOrNull { candidate -> candidate.name == status }
                ?: return null
        return DraftCompletionSignal(taskId = validTaskId, status = validStatus)
    }

    const val TASK_ID_KEY = "taskId"
    const val STATUS_KEY = "status"
}
