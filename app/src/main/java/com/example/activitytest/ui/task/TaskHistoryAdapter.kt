package com.example.activitytest.ui.task

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.activitytest.R
import com.example.activitytest.data.TaskHistory
import com.example.activitytest.databinding.ItemTaskHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TaskHistoryAdapter(
    private val onHistoryClick: (TaskHistory) -> Unit,
    private val onSelectionChanged: (Set<Long>) -> Unit
) : ListAdapter<TaskHistory, TaskHistoryAdapter.TaskHistoryViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<Long>()


    fun setSelectionMode(enabled: Boolean) {
        if (!enabled) {
            selectedIds.clear()
            onSelectionChanged(selectedIds)
            notifyDataSetChanged()
        }
    }

    fun toggleSelectAll() {
        if (selectedIds.size == currentList.size) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(currentList.map(TaskHistory::id))
        }
        onSelectionChanged(selectedIds.toSet())
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskHistoryViewHolder = TaskHistoryViewHolder(
        ItemTaskHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: TaskHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: TaskHistoryViewHolder) {
        super.onViewRecycled(holder)
        holder.clearRevealCallback()
    }

    inner class TaskHistoryViewHolder(
        private val binding: ItemTaskHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: TaskHistory) {
            binding.tvHistoryTitle.text = history.title
            binding.tvHistoryContent.text = history.content.ifBlank {
                binding.root.context.getString(R.string.no_task_description)
            }
            binding.tvDeletedAt.text = binding.root.context.getString(
                R.string.history_deleted_at,
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                ).format(Date(history.deletedAt))
            )

            val isSelected = history.id in selectedIds

            // 已选中的任务保持展开，露出选择框；未选中的任务默认收起，
            // 需要用户右滑才能看到选择框。
            binding.root.setRevealedImmediate(isSelected)

            binding.checkHistorySelected.setOnCheckedChangeListener(null)
            binding.checkHistorySelected.isChecked = isSelected
            binding.checkHistorySelected.setOnCheckedChangeListener { _, checked ->
                updateSelection(history.id, checked)
                // 勾选后保持展开；取消勾选则收起选择框
                if (checked) binding.root.reveal() else binding.root.collapse()
            }

            // 右滑手势结束后的展开状态变化：仅用于同步选择框可见性，
            // 具体是否选中仍由用户点击选择框决定。
            binding.root.onRevealStateChanged = { revealed ->
                if (!revealed && history.id in selectedIds) {
                    // 用户把已选中的任务收起，视为取消选择
                    updateSelection(history.id, false)
                }
            }

            binding.historyForeground.setOnClickListener {
                if (binding.root.isRevealed) {
                    binding.root.collapse()
                } else {
                    onHistoryClick(history)
                }
            }
        }

        fun clearRevealCallback() {
            binding.root.onRevealStateChanged = null
        }
    }

    private fun updateSelection(historyId: Long, isSelected: Boolean) {
        if (isSelected) {
            selectedIds += historyId
        } else {
            selectedIds -= historyId
        }
        onSelectionChanged(selectedIds.toSet())
    }

    private class DiffCallback : DiffUtil.ItemCallback<TaskHistory>() {
        override fun areItemsTheSame(oldItem: TaskHistory, newItem: TaskHistory) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TaskHistory, newItem: TaskHistory) =
            oldItem == newItem
    }
}
