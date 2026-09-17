package com.example.activitytest.ui.task

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import com.example.activitytest.util.MoreMenuItem
import com.example.activitytest.util.MoreMenuPopup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.example.activitytest.R
import com.example.activitytest.data.Task
import com.example.activitytest.service.SyncService
import com.example.activitytest.util.MenuDialogHelper
import com.example.activitytest.viewmodel.TaskViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.util.Log

class CompletedTaskFragment :
    Fragment(R.layout.fragment_task_list) {


    private val viewModel: TaskViewModel by viewModels()


    private lateinit var taskAdapter: TaskAdapter
    private val clickHandler = Handler(Looper.getMainLooper())
    private var pendingTaskId: Long? = null
    private var sortMode = MenuDialogHelper.SortMode.DEADLINE
    private var lastTasks: List<Task> = emptyList()
    private val openPendingTask = Runnable {
        pendingTaskId?.let(::openTaskDetailPage)
        pendingTaskId = null
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.fabAddTask).visibility = View.GONE

        // 复用任务列表布局，但标题与副标题需要替换为“已完成”页文案
        view.findViewById<TextView>(R.id.tvTitle)
            .setText(R.string.completed_title)
        view.findViewById<TextView>(R.id.tvSubtitle)
            .setText(R.string.completed_subtitle)

        setupRecyclerView(view)
        setupSearch(view)
        setupMoreMenu(view)
        setupHistory(view)

        observeTasks()

        com.example.activitytest.util.WindowInsetsHelper.applySystemBarPadding(
            view,
            applyTop = false,
            applyBottom = false
        )
    }


    private fun setupRecyclerView(view: View) {


        val recyclerView =
            view.findViewById<RecyclerView>(R.id.recyclerView)


        taskAdapter = TaskAdapter(


            onTaskClick = { task ->
                handleTaskClick(task.id)
            },


            onCompletedChange = { task, checked ->
                if (checked) {
                    // 勾选完成时，二次确认
                    showCompleteConfirmDialog(task)
                } else {
                    // 取消勾选（恢复任务）时，二次确认
                    showRestoreConfirmDialog(task)
                }
            },

            onEditClick = { task ->
                openTaskEditPage(task.id)
            },

            onDeleteClick = { task ->
                showDeleteDialog(task)
            }
        )

        recyclerView.adapter = taskAdapter


        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())
    }

    private fun setupSearch(view: View) {
        view.findViewById<ImageButton>(R.id.btnSearchTask).setOnClickListener {
            showSearchCompletedTasksDialog()
        }
    }

    private fun showSearchCompletedTasksDialog() {
        val contentView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_search_task,
            null,
            false
        )
        val taskTitleInput = contentView.findViewById<TextInputEditText>(R.id.etSearchTaskTitle)
        val feedbackView = contentView.findViewById<TextView>(R.id.tvSearchFeedback)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.search_completed_tasks)
            .setView(contentView)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.search, null)
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
            }

        fun searchTasks() {
            val keyword = taskTitleInput.text?.toString()?.trim().orEmpty()
            if (keyword.isEmpty()) {
                feedbackView.setText(R.string.search_task_title_invalid)
                feedbackView.visibility = View.VISIBLE
                return
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val tasks = viewModel.searchCompletedTasksByTitle(keyword)
                when (tasks.size) {
                    0 -> {
                        feedbackView.setText(R.string.search_task_not_found)
                        feedbackView.visibility = View.VISIBLE
                    }
                    1 -> {
                        dialog.dismiss()
                        openTaskDetailPage(tasks.first().id)
                    }
                    else -> {
                        taskAdapter.submitList(MenuDialogHelper.sortTasks(tasks, sortMode))
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { searchTasks() }
            taskTitleInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchTasks()
                    true
                } else false
            }
            taskTitleInput.requestFocus()
            taskTitleInput.post {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(taskTitleInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )
    }

    private fun setupMoreMenu(view: View) {
        val btnMore = view.findViewById<ImageButton>(R.id.btnMore)
        btnMore.setOnClickListener { anchor ->
            MoreMenuPopup.show(
                anchor,
                listOf(
                    MoreMenuItem(
                        R.id.menu_sort,
                        R.string.menu_sort,
                        R.drawable.ic_sort
                    ),
                    MoreMenuItem(
                        R.id.menu_sync,
                        R.string.menu_sync,
                        R.drawable.ic_sync
                    )
                )
            ) { itemId ->
                when (itemId) {
                    R.id.menu_sort -> MenuDialogHelper.showSortDialog(
                        requireActivity(),
                        sortMode
                    ) { mode ->
                        sortMode = mode
                        renderTasks(lastTasks)
                    }

                    R.id.menu_sync -> {
                        val intent = Intent(
                            requireContext(),
                            SyncService::class.java
                        )
                        requireContext().startService(intent)
                    }
                }
            }
        }
    }

    private fun setupHistory(view: View) {
        val historyButton = view.findViewById<TextView>(R.id.btnHistory)
        historyButton.visibility = View.VISIBLE
        historyButton.setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun showHistoryDialog() {
        val contentView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_task_history,
            null,
            false
        )
        val historyList = contentView.findViewById<RecyclerView>(R.id.recyclerHistory)
        val emptyView = contentView.findViewById<TextView>(R.id.tvHistoryEmpty)
        val selectAllBox = contentView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(
            R.id.checkSelectAllHistory
        )
        val selectedIds = mutableSetOf<Long>()
        lateinit var dialog: AlertDialog

        lateinit var historyAdapter: TaskHistoryAdapter
        historyAdapter = TaskHistoryAdapter(
            onHistoryClick = { history ->
                startActivity(Intent(requireContext(), TaskDetailActivity::class.java).apply {
                    putExtra(TaskDetailActivity.EXTRA_HISTORY_ID, history.id)
                })
            },
            onSelectionChanged = { ids ->
                selectedIds.clear()
                selectedIds.addAll(ids)
                selectAllBox.setOnCheckedChangeListener(null)
                selectAllBox.isChecked = ids.isNotEmpty() && ids.size == historyAdapter.currentList.size
                selectAllBox.setOnCheckedChangeListener { _, _ ->
                    historyAdapter.toggleSelectAll()
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = ids.isNotEmpty()
            }
        )

        historyList.layoutManager = LinearLayoutManager(requireContext())
        historyList.adapter = historyAdapter

        dialog = AlertDialog.Builder(requireContext())
            .setView(contentView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        selectAllBox.setOnCheckedChangeListener { _, _ ->
            historyAdapter.setSelectionMode(true)
            historyAdapter.toggleSelectAll()
        }

        val historyJob: Job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deletedTasks.collect { histories ->
                historyAdapter.submitList(histories)
                emptyView.visibility = if (histories.isEmpty()) View.VISIBLE else View.GONE
                historyList.visibility = if (histories.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setText(R.string.history_restore)
                isEnabled = false
                setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.restore_history_title)
                        .setMessage(R.string.restore_history_confirm)
                        .setNegativeButton(R.string.no, null)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.restoreHistories(selectedIds.toList())
                            historyAdapter.setSelectionMode(false)
                            selectAllBox.setOnCheckedChangeListener(null)
                            selectAllBox.isChecked = false
                            selectAllBox.setOnCheckedChangeListener { _, _ ->
                                historyAdapter.setSelectionMode(true)
                                historyAdapter.toggleSelectAll()
                            }
                        }
                        .show()
                }
            }
        }
        dialog.setOnDismissListener {
            historyJob.cancel()
            historyList.adapter = null
        }
        dialog.show()
    }

    private fun handleTaskClick(taskId: Long) {
        if (pendingTaskId != null) {
            clickHandler.removeCallbacks(openPendingTask)
            pendingTaskId = null
            return
        }

        pendingTaskId = taskId
        clickHandler.postDelayed(openPendingTask, 180)
    }

    private fun openTaskDetailPage(taskId: Long) {
        val intent = Intent(
            requireContext(),
            TaskDetailActivity::class.java
        ).apply {
            putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId)
        }
        startActivity(intent)
    }

    private fun openTaskEditPage(taskId: Long) {
        val intent = Intent(
            requireContext(),
            TaskEditActivity::class.java
        ).apply {
            putExtra(TaskEditActivity.EXTRA_TASK_ID, taskId)
        }
        startActivity(intent)
    }

    private fun showCompleteConfirmDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.complete_task_confirm)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.updateCompleted(task.id, true)
            }
            .show()
    }

    private fun showRestoreConfirmDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.restore_task_confirm)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.updateCompleted(task.id, false)
            }
            .show()
    }

    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_task_title)
            .setMessage(
                getString(R.string.delete_task_confirm_message, task.title)
            )
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteTask(task)
            }
            .show()
    }


    override fun onDestroyView() {
        clickHandler.removeCallbacks(openPendingTask)
        pendingTaskId = null
        super.onDestroyView()
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.completedTasks.collect { tasks ->
                    renderTasks(tasks)
                }
            }
        }
    }

    private fun renderTasks(tasks: List<Task>) {
        lastTasks = tasks
        taskAdapter.submitList(
            MenuDialogHelper.sortTasks(tasks, sortMode)
        )
    }
}