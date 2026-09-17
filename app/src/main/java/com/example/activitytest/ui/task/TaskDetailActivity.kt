package com.example.activitytest.ui.task

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.activitytest.R
import com.example.activitytest.data.Task
import com.example.activitytest.data.TaskHistory
import com.example.activitytest.databinding.ActivityTaskDetailBinding
import com.example.activitytest.receiver.ReminderReceiver
import com.example.activitytest.viewmodel.TaskViewModel
import com.example.activitytest.util.ThemeHelper
import com.example.activitytest.util.WindowInsetsHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

class TaskDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_HISTORY_ID = "extra_history_id"
        private const val TAG = "TaskDetailActivity"
    }

    private lateinit var binding: ActivityTaskDetailBinding

    private val viewModel: TaskViewModel by viewModels()

    private var taskId: Long = -1L
    private var historyId: Long = -1L

    private var currentTask: Task? = null

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "nihao onStart")
    }

    override fun onPause() {
        Log.d(TAG, "nihao onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "nihao onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "nihao onDestroy")
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityTaskDetailBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        WindowInsetsHelper.applySystemBarPadding(binding.root)

        /*
         * 从启动详情页的 Intent 中获取任务 ID。
         */
        historyId = intent.getLongExtra(EXTRA_HISTORY_ID, -1L)
        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        Log.d(TAG, "nihao onCreate")

        if (historyId == -1L && taskId == -1L) {
            Toast.makeText(
                this,
                R.string.task_invalid_id,
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        /*
         * 设置按钮点击事件。
         */
        binding.btnEditTask.setOnClickListener {
            openEditPage()
        }

        binding.btnDeleteTask.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.btnToggleCompleted.setOnClickListener {
            toggleCompleted()
        }

        binding.btnTestReminder.setOnClickListener {
            sendTestReminder()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (historyId != -1L) {
            setupHistoryDetail()
            loadHistory()
        } else {
            loadTask()
        }
    }

    private fun setupHistoryDetail() {
        binding.tvPageTitle.setText(R.string.history_task_detail_title)
        binding.btnToggleCompleted.visibility = android.view.View.GONE
        binding.btnEditTask.visibility = android.view.View.GONE
        binding.btnTestReminder.visibility = android.view.View.GONE
        binding.btnDeleteTask.visibility = android.view.View.GONE
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val history = viewModel.getHistoryById(historyId)
            if (history == null) {
                Toast.makeText(
                    this@TaskDetailActivity,
                    R.string.task_not_found,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }
            displayHistory(history)
        }
    }

    private fun displayHistory(history: TaskHistory) {
        binding.tvTaskTitle.text = history.title
        binding.tvTaskContent.text = history.content.ifBlank {
            getString(R.string.no_task_description)
        }
        binding.tvCompletedStatus.text = getString(
            R.string.completed_status_format,
            if (history.isCompleted) getString(R.string.completed) else getString(R.string.not_completed)
        )
        binding.tvPriority.text = getString(
            R.string.priority_format,
            getString(priorityLabel(history.priority))
        )
        binding.tvDeadline.text = history.deadline?.let { deadline ->
            getString(R.string.deadline_time_format, formatTime(deadline))
        } ?: getString(R.string.no_deadline)
        binding.tvCreatedAt.text = getString(
            R.string.created_time_format,
            formatTime(history.createdAt)
        )
    }

    /**
     * 从 Room 中查询任务。
     */
    private fun loadTask() {
        lifecycleScope.launch {
            val task = viewModel.getTaskById(taskId)

            if (task == null) {
                Toast.makeText(
                    this@TaskDetailActivity,
                    R.string.task_not_found,
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                return@launch
            }

            currentTask = task

            displayTask(task)
        }
    }

    /**
     * 将任务内容显示到页面。
     */
    private fun displayTask(task: Task) {
        binding.tvTaskTitle.text = task.title

        binding.tvTaskContent.text =
            task.content.ifBlank {
                getString(R.string.no_task_description)
            }

        binding.tvCompletedStatus.text = getString(
            R.string.completed_status_format,
            if (task.isCompleted) getString(R.string.completed) else getString(R.string.not_completed)
        )

        // 按钮文字随完成状态变化：已完成显示「标记未完成」，反之「标记完成」
        binding.btnToggleCompleted.text =
            if (task.isCompleted) {
                getString(R.string.mark_incomplete)
            } else {
                getString(R.string.mark_complete)
            }

        binding.tvPriority.text = getString(
            R.string.priority_format,
            getString(priorityLabel(task.priority))
        )
        binding.tvDeadline.text =
            task.deadline?.let { deadline ->
                getString(
                    R.string.deadline_time_format,
                    formatTime(deadline)
                )
            } ?: getString(R.string.no_deadline)
        binding.tvCreatedAt.text = getString(
            R.string.created_time_format,
            formatTime(task.createdAt)
        )
    }

    private fun priorityLabel(priority: Int): Int = when (priority) {
        2 -> R.string.priority_value_high
        1 -> R.string.priority_value_normal
        else -> R.string.priority_value_low
    }

    /**
     * 时间戳格式化。
     */
    private fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        )

        return formatter.format(Date(timestamp))
    }

    /**
     * 点击“编辑任务”后进入编辑页面。
     */
    private fun openEditPage() {
        val task = currentTask

        if (task == null) {
            Toast.makeText(
                this,
                R.string.task_not_loaded,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent = Intent(
            this,
            TaskEditActivity::class.java
        ).apply {
            putExtra(
                TaskEditActivity.EXTRA_TASK_ID,
                task.id
            )
        }

        startActivity(intent)
    }

    /**
     * 切换任务的完成状态。
     */
    private fun toggleCompleted() {
        val task = currentTask ?: return

        // 取反：当前完成→未完成，当前未完成→完成
        val newCompleted = !task.isCompleted

        // 更新数据库
        viewModel.updateCompleted(task.id, newCompleted)

        // 立即更新本地缓存和界面，避免等待数据库回调
        currentTask = task.copy(isCompleted = newCompleted)
        displayTask(task.copy(isCompleted = newCompleted))

        Toast.makeText(
            this,
            if (newCompleted) {
                R.string.marked_complete
            } else {
                R.string.marked_incomplete
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 点击“删除任务”后弹出二次确认。
     */
    private fun showDeleteConfirmDialog() {
        val task = currentTask ?: return

        AlertDialog.Builder(this)
            .setTitle(R.string.delete_task_title)
            .setMessage(
                getString(R.string.delete_task_confirm_detail, task.title)
            )
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->

                viewModel.deleteTask(task)

                Toast.makeText(
                    this,
                    R.string.task_deleted,
                    Toast.LENGTH_SHORT
                ).show()

                setResult(RESULT_OK)

                finish()
            }
            .show()
    }

    /**
     * 发送显式广播，触发 ReminderReceiver。
     */
    private fun sendTestReminder() {
        val task = currentTask

        if (task == null) {
            Toast.makeText(
                this,
                R.string.task_not_loaded,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent = Intent(
            this,
            ReminderReceiver::class.java
        ).apply {
            putExtra(
                ReminderReceiver.EXTRA_TASK_ID,
                task.id
            )

            putExtra(
                ReminderReceiver.EXTRA_TASK_TITLE,
                task.title
            )
        }

        sendBroadcast(intent)

        Toast.makeText(
            this,
            R.string.test_reminder_sent,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 从编辑页面返回时重新查询数据库，
     * 确保详情页显示最新内容。
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "nihao onResume")

        if (historyId == -1L && taskId != -1L) {
            loadTask()
        }
    }
}