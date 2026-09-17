package com.example.activitytest.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    // 0 表示未设置id，在插入数据时会自动生成一个id
    val id: Long = 0,
    // 任务标题
    val title: String,
    // 任务内容=description任务描述
    val content: String = "",
    val description: String ="",
    // 截止日期
    val deadline: Long? = null,
    // 是否完成
    val isCompleted: Boolean = false,
    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),
// 优先级
    val priority: Int = 0,
    // 最后修改时间
    val updatedAt: Long = System.currentTimeMillis(),
)