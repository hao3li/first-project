package com.example.activitytest.util

import android.app.Activity
import android.app.AlertDialog
import com.example.activitytest.R
import com.example.activitytest.data.Task


object MenuDialogHelper {

    /** 排序方式 */
    enum class SortMode(val titleRes: Int) {
        DEADLINE(R.string.sort_by_deadline),
        CREATED(R.string.sort_by_created),
        PRIORITY(R.string.sort_by_priority)
    }


    fun showSortDialog(
        activity: Activity,
        current: SortMode,
        onSelected: (SortMode) -> Unit
    ) {
        val modes = SortMode.values()
        val currentIndex = modes.indexOf(current).coerceAtLeast(0)

        AlertDialog.Builder(activity)
            .setTitle(R.string.sort_title)
            .setSingleChoiceItems(
                modes.map { activity.getString(it.titleRes) }.toTypedArray(),
                currentIndex
            ) { dialog, which ->
                onSelected(modes[which])
                dialog.dismiss()
            }
            .show()
    }


    fun showLanguageDialog(
        activity: Activity,
        onLanguageChanged: (() -> Unit)? = null
    ) {
        val options = arrayOf(
            activity.getString(R.string.language_chinese),
            activity.getString(R.string.language_english)
        )

        // 当前语言决定高亮项：中文 -> 0，英文 -> 1
        val currentIndex = if (LocaleHelper.isChinese()) 0 else 1

        AlertDialog.Builder(activity)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val target = if (which == 1) {
                    LocaleHelper.LANG_EN
                } else {
                    LocaleHelper.LANG_ZH
                }

                if (LocaleHelper.setLanguage(target)) {
                    onLanguageChanged?.invoke()
                }
                dialog.dismiss()
            }
            .show()
    }


    fun sortTasks(tasks: List<Task>, mode: SortMode): List<Task> {
        return tasks.sortedWith(
            when (mode) {
                SortMode.DEADLINE -> compareBy<Task> { it.deadline ?: Long.MAX_VALUE }
                    .thenBy { it.createdAt }
                SortMode.CREATED -> compareByDescending<Task> { it.createdAt }
                    .thenByDescending { it.updatedAt }
                SortMode.PRIORITY -> compareByDescending<Task> { it.priority }
                    .thenBy { it.deadline ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            }
        )
    }
}
