package com.example.activitytest.ui.task

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.activitytest.R
import com.example.activitytest.data.Task
import com.example.activitytest.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// 任务列表适配器
class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onCompletedChange: (Task, Boolean) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {


    // 创建任务ViewHolder
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TaskViewHolder(binding)
    }

    // 绑定任务ViewHolder
    override fun onBindViewHolder(
        holder: TaskViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))  // 获取当前位置的任务
        holder.collapseActions()  // 复用前先收起，避免残留展开状态
    }

    // 任务ViewHolder
    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {  // 继承RecyclerView.ViewHolder


        // 收起前景卡片，还原到初始位置
        fun collapseActions() {
            binding.cardForeground.collapse()
        }

        // 绑定任务数据
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title

            val formatter = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            )
            binding.tvDeadline.text = task.deadline?.let {
                binding.root.context.getString(
                    R.string.deadline_prefix,
                    formatter.format(Date(it))
                )
            } ?: binding.root.context.getString(R.string.no_deadline)
            binding.tvTaskMeta.text = binding.root.context.getString(
                R.string.task_meta,
                formatter.format(Date(task.createdAt)),
                binding.root.context.getString(priorityLabel(task.priority))
            )

            // 先移除监听器，防止 RecyclerView 复用时错误触发
            binding.checkCompleted.setOnCheckedChangeListener(null)
            binding.checkCompleted.isChecked = task.isCompleted

            // 设置任务标题的字体样式
            if (task.isCompleted) {
                binding.tvTaskTitle.paintFlags =
                    binding.tvTaskTitle.paintFlags or
                            Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskTitle.paintFlags =
                    binding.tvTaskTitle.paintFlags and
                            Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // 设置任务完成状态的监听器
            binding.checkCompleted.setOnCheckedChangeListener { _, checked ->
                // 先把复选框恢复为真实状态，避免 UI 提前变化
                // 是否真正完成由上层确认后再通过数据流刷新
                binding.checkCompleted.isChecked = task.isCompleted
                onCompletedChange(task, checked)
            }

            // 设置任务内容的点击监听器
            binding.taskContentContainer.setOnClickListener {
                onTaskClick(task)
            }

            // 设置编辑按钮的点击监听器（左滑后露出）
            binding.btnEdit.setOnClickListener {
                onEditClick(task)
            }

            // 设置删除按钮的点击监听器（左滑后露出）
            binding.btnDelete.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }

    private fun priorityLabel(priority: Int): Int = when (priority) {
        2 -> R.string.priority_value_high
        1 -> R.string.priority_value_normal
        else -> R.string.priority_value_low
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {


        override fun areItemsTheSame(
            oldItem: Task,
            newItem: Task
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Task,
            newItem: Task
        ): Boolean {
            return oldItem == newItem
        }
    }
}
