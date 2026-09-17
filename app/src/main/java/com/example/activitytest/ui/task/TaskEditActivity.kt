package com.example.activitytest.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.activitytest.R
import com.example.activitytest.data.Task
import com.example.activitytest.databinding.ActivityTaskEditBinding
import com.example.activitytest.viewmodel.TaskViewModel
import com.example.activitytest.util.ThemeHelper
import com.example.activitytest.util.WindowInsetsHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val TAG = "TaskEditActivity"

        private const val STATE_SELECTED_DEADLINE =
            "state_selected_deadline"
        private const val PRIORITY_LOW = 0
        private const val PRIORITY_NORMAL = 1
        private const val PRIORITY_HIGH = 2
    }

    private lateinit var binding: ActivityTaskEditBinding

    private val viewModel: TaskViewModel by viewModels()

    private var taskId: Long = -1L
    private var selectedDeadline: Long? = null
    private var selectedPriority = PRIORITY_NORMAL
    private var oldTask: Task? = null
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding = ActivityTaskEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupKeyboardDismissGesture()

        WindowInsetsHelper.applySystemBarPadding(binding.root)

        taskId = intent.getLongExtra(
            EXTRA_TASK_ID,
            -1L
        )

        /*
         * 如果屏幕旋转过，恢复用户之前选择的截止时间。
         */
        if (
            savedInstanceState?.containsKey(
                STATE_SELECTED_DEADLINE
            ) == true
        ) {
            selectedDeadline =
                savedInstanceState.getLong(
                    STATE_SELECTED_DEADLINE
                )
        }

        if (taskId == -1L) {
            binding.tvPageTitle.setText(R.string.add_task_title)

            updateDeadlineText()
            updatePriorityText()
        } else {
            binding.tvPageTitle.setText(R.string.edit_task_title)

            loadTask(taskId)
        }

        binding.btnSelectDeadline.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnSelectPriority.setOnClickListener {
            showPriorityDialog()
        }

        binding.btnSave.setOnClickListener {
            saveTask()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        Log.d(TAG, "onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }


    private fun setupKeyboardDismissGesture() {
        var touchDownY = 0f
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> touchDownY = event.rawY
                MotionEvent.ACTION_UP -> {
                    if (event.rawY - touchDownY > touchSlop) {
                        val inputMethodManager = getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        ) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(
                            binding.root.windowToken,
                            0
                        )
                        currentFocus?.clearFocus()
                    }
                }
            }
            false
        }
    }

    private fun showPriorityDialog() {
        val priorityLabels = arrayOf(
            getString(R.string.priority_low),
            getString(R.string.priority_normal),
            getString(R.string.priority_high)
        )
        val checkedIndex = when (selectedPriority) {
            PRIORITY_HIGH -> 2
            PRIORITY_NORMAL -> 1
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.priority_title)
            .setSingleChoiceItems(priorityLabels, checkedIndex) { dialog, which ->
                selectedPriority = when (which) {
                    2 -> PRIORITY_HIGH
                    1 -> PRIORITY_NORMAL
                    else -> PRIORITY_LOW
                }
                updatePriorityText()
                dialog.dismiss()
            }
            .show()
    }

    private fun updatePriorityText() {
        binding.btnSelectPriority.setText(
            when (selectedPriority) {
                PRIORITY_HIGH -> R.string.priority_high
                PRIORITY_LOW -> R.string.priority_low
                else -> R.string.priority_normal
            }
        )
    }

    private fun loadTask(taskId: Long) {
        lifecycleScope.launch {
            val task = viewModel.getTaskById(taskId)

            if (task == null) {
                Toast.makeText(
                    this@TaskEditActivity,
                    R.string.task_not_found,
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                return@launch
            }

            oldTask = task

            /*
             * 如果不是屏幕旋转恢复的截止时间，
             * 才使用数据库中的截止时间。
             */
            if (selectedDeadline == null) {
                selectedDeadline = task.deadline
            }

            binding.etTitle.setText(task.title)
            binding.etContent.setText(task.content)
            binding.checkCompleted.isChecked =
                task.isCompleted
            selectedPriority = task.priority.coerceIn(PRIORITY_LOW, PRIORITY_HIGH)

            updateDeadlineText()
            updatePriorityText()
        }
    }

    /**
     * 保存新增任务或更新已有任务。
     */
    private fun saveTask() {
        val title = binding.etTitle.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val content = binding.etContent.text
            ?.toString()
            ?.trim()
            .orEmpty()

        // 检查标题
        if (title.isBlank()) {
            binding.titleInputLayout.error =
                getString(R.string.title_required)
            return
        }

        binding.titleInputLayout.error = null

        // 检查截止时间
        val deadline = selectedDeadline

        if (
            deadline != null &&
            deadline <= System.currentTimeMillis()
        ) {
            Toast.makeText(
                this,
                R.string.deadline_in_future,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // 防止用户重复点击保存
        if (isSaving) {
            return
        }

        isSaving = true
        binding.btnSave.isEnabled = false

        if (taskId == -1L) {
            // 没有任务编号，表示新增任务
            val newTask = Task(
                title = title,
                content = content,
                deadline = selectedDeadline,
                isCompleted =
                    binding.checkCompleted.isChecked,
                priority = selectedPriority
            )

            viewModel.insertTask(newTask)
        } else {
            // 有任务编号，表示修改任务
            val original = oldTask

            if (original == null) {
                isSaving = false
                binding.btnSave.isEnabled = true

                Toast.makeText(
                    this,
                    R.string.task_not_loaded,
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val updatedTask = original.copy(
                title = title,
                content = content,
                deadline = selectedDeadline,
                isCompleted =
                    binding.checkCompleted.isChecked,
                priority = selectedPriority,
                updatedAt = System.currentTimeMillis()
            )

            viewModel.updateTask(updatedTask)
        }

        setResult(RESULT_OK)

        Toast.makeText(
            this,
            if (taskId == -1L) {
                R.string.task_added_success
            } else {
                R.string.task_updated_success
            },
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    /**
     * 先选择日期，再选择时间。
     */
    private fun showDateTimePicker() {
        /*
         * 已经选择过截止时间时，以原时间为默认值；
         * 否则以当前时间为默认值。
         */
        val calendar = Calendar.getInstance().apply {  //apply 异步保存，不阻塞当前线程
            selectedDeadline?.let { deadline ->
                timeInMillis = deadline
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->

                // 选择日期之后继续选择时间
                val hour =
                    calendar.get(Calendar.HOUR_OF_DAY)

                val minute =
                    calendar.get(Calendar.MINUTE)

                val timePickerDialog =
                    TimePickerDialog(
                        this,
                        { _, selectedHour, selectedMinute ->

                            calendar.set(
                                selectedYear,
                                selectedMonth,
                                selectedDay,
                                selectedHour,
                                selectedMinute,
                                0
                            )

                            calendar.set(
                                Calendar.MILLISECOND,
                                0
                            )

                            val deadline =
                                calendar.timeInMillis

                            if (
                                deadline <=
                                System.currentTimeMillis()
                            ) {
                                Toast.makeText(
                                    this@TaskEditActivity,
                                    R.string.deadline_in_future,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                selectedDeadline = deadline
                                updateDeadlineText()
                            }
                        },
                        hour,
                        minute,
                        true
                    )

                timePickerDialog.setTitle(
                    getString(R.string.select_deadline_time)
                )

                timePickerDialog.show()
            },
            year,
            month,
            day
        )

        datePickerDialog.setTitle(
            getString(R.string.select_deadline_date)
        )

        /*
         * 禁止选择今天之前的日期。
         * 减去1000毫秒，避免部分设备把今天判断成不可选。
         */
        datePickerDialog.datePicker.minDate =
            System.currentTimeMillis() - 1000L

        datePickerDialog.show()
    }

    /**
     * 将 Long 时间戳转换为页面文字。
     */
    private fun updateDeadlineText() {
        binding.tvSelectedDeadline.text =
            selectedDeadline?.let { deadline ->
                val formatter = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                )

                getString(
                    R.string.deadline_time_format,
                    formatter.format(Date(deadline))
                )
            } ?: getString(R.string.no_deadline)
    }

    /**
     * 屏幕旋转时保存当前选择的截止时间。
     */
    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        Log.d(TAG, "onSaveInstanceState")
        selectedDeadline?.let { deadline ->
            outState.putLong(
                STATE_SELECTED_DEADLINE,
                deadline
            )
        }

        super.onSaveInstanceState(outState)
    }
}
