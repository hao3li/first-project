package com.example.activitytest.ui.task

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.example.activitytest.util.MoreMenuItem
import com.example.activitytest.util.MoreMenuPopup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.example.activitytest.R
import com.example.activitytest.databinding.FragmentTaskListBinding
import com.example.activitytest.service.SyncService
import com.example.activitytest.util.MenuDialogHelper
import com.example.activitytest.viewmodel.TaskViewModel

class TaskListFragment : Fragment(R.layout.fragment_task_list) {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskViewModel by viewModels()

    private lateinit var taskAdapter: TaskAdapter
    private val clickHandler = Handler(Looper.getMainLooper())
    private var pendingTaskId: Long? = null
    private var sortMode = MenuDialogHelper.SortMode.DEADLINE
    private val openPendingTask = Runnable {
        pendingTaskId?.let(::openTaskDetailPage)
        pendingTaskId = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTaskListBinding.bind(view)

        setupRecyclerView()
        setupClickListeners()
        setupMoreMenu()
        observeTasks()

        com.example.activitytest.util.WindowInsetsHelper.applySystemBarPadding(
            binding.root,
            applyTop = false,
            applyBottom = false
        )
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                handleTaskClick(task.id)
            },

            onCompletedChange = { task, completed ->
                if (completed) {
                    // 勾选完成时，二次确认
                    showCompleteConfirmDialog(task)
                } else {
                    // 取消勾选，直接更新
                    viewModel.updateCompleted(task.id, false)
                }
            },

            onEditClick = { task ->
                openTaskEditPage(task.id)
            },

            onDeleteClick = { task ->
                showDeleteDialog(task)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()) // 使用线性布局管理器
            adapter = taskAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTask.setOnClickListener {
            openTaskEditPage(null)
        }
        binding.btnSearchTask.setOnClickListener {
            showSearchTaskDialog()
        }
    }

    private fun showSearchTaskDialog() {
        val contentView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_search_task,
            null,
            false
        )
        val taskTitleInput = contentView.findViewById<TextInputEditText>(R.id.etSearchTaskTitle)
        val feedbackView = contentView.findViewById<android.widget.TextView>(R.id.tvSearchFeedback)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.search_task)
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
                val tasks = viewModel.searchTasksByTitle(keyword)
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
                        taskAdapter.submitList(
                            MenuDialogHelper.sortTasks(tasks, sortMode)
                        )
                        binding.tvEmpty.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.setOnShowListener {
            taskTitleInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    feedbackView.visibility = View.GONE
                }

                override fun afterTextChanged(s: Editable?) = Unit
            })
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                searchTasks()
            }
            taskTitleInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchTasks()
                    true
                } else {
                    false
                }
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

    private fun setupMoreMenu() {
        binding.btnMore.setOnClickListener { anchor ->
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
                        refreshCurrentList()
                    }

                    R.id.menu_sync -> startSync()
                }
            }
        }
    }

    private fun startSync() {
        val intent = Intent(requireContext(), SyncService::class.java)
        requireContext().startService(intent)
    }

    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.uncompletedTasks.collect { tasks ->
                    renderTasks(tasks)
                }
            }
        }
    }

    // 缓存最近一次任务列表，便于切换排序方式时立即刷新
    private var lastTasks: List<com.example.activitytest.data.Task> = emptyList()

    private fun renderTasks(tasks: List<com.example.activitytest.data.Task>) {
        lastTasks = tasks
        taskAdapter.submitList(
            MenuDialogHelper.sortTasks(tasks, sortMode)
        )

        binding.tvEmpty.visibility =
            if (tasks.isEmpty()) View.VISIBLE
            else View.GONE

        binding.recyclerView.visibility =
            if (tasks.isEmpty()) View.GONE
            else View.VISIBLE
    }

    private fun refreshCurrentList() {
        renderTasks(lastTasks)
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

    private fun openTaskEditPage(taskId: Long?) {
        val intent = Intent(
            requireContext(),
            TaskEditActivity::class.java
        )

        taskId?.let {
            intent.putExtra(TaskEditActivity.EXTRA_TASK_ID, it)
        }

        startActivity(intent)
    }

    private fun showCompleteConfirmDialog(
        task: com.example.activitytest.data.Task
    ) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.complete_task_confirm)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.updateCompleted(task.id, true)
            }
            .show()
    }

    private fun showDeleteDialog(
        task: com.example.activitytest.data.Task
    ) {
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
        binding.recyclerView.adapter = null
        _binding = null
    }
}