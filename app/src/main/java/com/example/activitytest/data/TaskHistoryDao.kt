package com.example.activitytest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskHistoryDao {

    @Query("SELECT * FROM task_history ORDER BY deletedAt DESC")
    fun observeDeletedTasks(): Flow<List<TaskHistory>>

    @Query("SELECT * FROM task_history WHERE id = :historyId LIMIT 1")
    suspend fun getHistoryById(historyId: Long): TaskHistory?

    @Query("SELECT * FROM task_history WHERE id IN (:historyIds)")
    suspend fun getHistoriesByIds(historyIds: List<Long>): List<TaskHistory>

    @Insert
    suspend fun insert(history: TaskHistory)

    @Insert
    suspend fun insertTasks(tasks: List<Task>)

    @Delete
    suspend fun deleteHistories(histories: List<TaskHistory>)

    @Transaction
    suspend fun restoreHistories(historyIds: List<Long>) {
        val histories = getHistoriesByIds(historyIds)
        if (histories.isEmpty()) return

        insertTasks(histories.map(TaskHistory::toRestoredTask))
        deleteHistories(histories)
    }
}
