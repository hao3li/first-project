package com.example.activitytest.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.activitytest.R
import com.example.activitytest.notification.NotificationHelper
import com.example.activitytest.ui.task.TaskDetailActivity

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val taskId = intent.getLongExtra(
            EXTRA_TASK_ID,
            -1L
        )

        val taskTitle = intent.getStringExtra(
            EXTRA_TASK_TITLE
        ) ?: context.getString(R.string.task_reminder_default_title)

        val detailIntent = Intent(
            context,
            TaskDetailActivity::class.java
        ).apply {
            putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE  //表示pendingintent创建后不能被其他组件修改，增强安全性
        )

        val notification = NotificationCompat.Builder(
            context,
            NotificationHelper.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.task_reminder_title))
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(taskId.toInt(), notification)
        } catch (_: SecurityException) {
        }
    }
}