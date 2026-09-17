package com.example.activitytest.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Transaction

//TaskDao 负责对tasks 表进行增删改查操作

@Dao
interface TaskDao {
    // 查询所有任务  Flow 表示数据发生变化时会自动通知观察者
    // 按完成状态升序排列 未完成排前面
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, deadline ASC")
    fun observeAllTasks(): Flow<List<Task>>

    // 查询未完成的任务
    // 按截止时间从早到晚排序
    @Query("""
        SELECT * FROM tasks
        WHERE isCompleted = 0
        ORDER BY deadline ASC
    """)
    fun observeUncompletedTasks(): Flow<List<Task>>

    // 查询已完成的任务 最近更新在前
    @Query("""
    SELECT * FROM tasks
        WHERE isCompleted = 1
        ORDER BY updatedAt DESC
    """)

    fun observeCompletedTasks(): Flow<List<Task>>


    // 根据id查询单个任务

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): Task?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND title LIKE '%' || :keyword || '%' COLLATE NOCASE ORDER BY deadline ASC")
    suspend fun searchTasksByTitle(keyword: String): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND title LIKE '%' || :keyword || '%' COLLATE NOCASE ORDER BY updatedAt DESC")
    suspend fun searchCompletedTasksByTitle(keyword: String): List<Task>

    // 插入任务
    @Insert
    suspend fun insertTask(task: Task): Long


    // 更新任务
    @Update
    suspend fun updateTask(task: Task)

    // 删除任务
    @Delete
    suspend fun deleteTask(task: Task)

    @Transaction
    suspend fun archiveAndDeleteTask(task: Task) {
        insertHistory(TaskHistory.fromTask(task))
        deleteTask(task)
    }

    @Insert
    suspend fun insertHistory(history: TaskHistory)

    // 更新任务完成状态
    // 只更新isCompleted和updatedAt字段
    @Query("""
        UPDATE tasks
        SET isCompleted = :completed,
            updatedAt = :updatedAt
        WHERE id = :taskId
    """)
    suspend fun updateCompleted(
        taskId: Long,
        completed: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
}