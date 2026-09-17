package com.example.activitytest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.activitytest.data.AppDatabase
import com.example.activitytest.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// 获取TaskDao，执行新增、修改、删除等操作

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val taskDao = database.taskDao()
    private val taskHistoryDao = database.taskHistoryDao()

    val uncompletedTasks: Flow<List<Task>> =
        taskDao.observeUncompletedTasks() // 查询未完成任务

    val completedTasks: Flow<List<Task>> =
        taskDao.observeCompletedTasks()   // 查询已完成任务

    val deletedTasks = taskHistoryDao.observeDeletedTasks()

    // 根据ID查询单个任务
    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun searchTasksByTitle(keyword: String): List<Task> {
        return taskDao.searchTasksByTitle(keyword)
    }

    suspend fun searchCompletedTasksByTitle(keyword: String): List<Task> {
        return taskDao.searchCompletedTasksByTitle(keyword)
    }

    suspend fun getHistoryById(historyId: Long): com.example.activitytest.data.TaskHistory? {
        return taskHistoryDao.getHistoryById(historyId)
    }

    fun restoreHistories(historyIds: List<Long>) {
        viewModelScope.launch {
            taskHistoryDao.restoreHistories(historyIds)
        }
    }

    // 新增任务
    fun insertTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
        }
    }

    // 更新任务
    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }


    // 删除任务
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.archiveAndDeleteTask(task)
        }
    }


    // 修改任务完成状态 勾选/取消勾选任务框调用
    fun updateCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskDao.updateCompleted(
                taskId,
                completed,
                System.currentTimeMillis()
            )
        }
    }
}