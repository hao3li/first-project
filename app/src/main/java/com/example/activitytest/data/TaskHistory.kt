package com.example.activitytest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_history")
data class TaskHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalTaskId: Long,
    val title: String,
    val content: String,
    val description: String,
    val deadline: Long?,
    val isCompleted: Boolean,
    val createdAt: Long,
    val priority: Int,
    val updatedAt: Long,
    val deletedAt: Long = System.currentTimeMillis()
) {
    fun toRestoredTask() = Task(
        title = title,
        content = content,
        description = description,
        deadline = deadline,
        isCompleted = isCompleted,
        createdAt = createdAt,
        priority = priority,
        updatedAt = System.currentTimeMillis()
    )

    companion object {
        fun fromTask(task: Task) = TaskHistory(
            originalTaskId = task.id,
            title = task.title,
            content = task.content,
            description = task.description,
            deadline = task.deadline,
            isCompleted = task.isCompleted,
            createdAt = task.createdAt,
            priority = task.priority,
            updatedAt = task.updatedAt
        )
    }
}
