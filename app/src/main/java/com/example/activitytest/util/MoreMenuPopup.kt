package com.example.activitytest.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.setPadding
import com.example.activitytest.R

data class MoreMenuItem(
    val id: Int,
    val titleRes: Int,
    val iconRes: Int
)

object MoreMenuPopup {

    private var activePopup: PopupWindow? = null

    fun show(
        anchor: View,
        items: List<MoreMenuItem>,
        onItemClick: (Int) -> Unit
    ) {
        activePopup?.let { popup ->
            popup.dismiss()
            return
        }

        val context = anchor.context
        val popupWidth = calculatePopupWidth(context, items)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(
                AppCompatResources.getColorStateList(
                    context,
                    R.color.surface_card
                )?.defaultColor ?: Color.WHITE
            )
            setPadding(context.dp(8))
        }

        lateinit var popup: PopupWindow
        items.forEach { item ->
            content.addView(
                createMenuItem(context, item) {
                    popup.dismiss()
                    onItemClick(item.id)
                }
            )
        }

        popup = PopupWindow(
            content,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = context.dp(8).toFloat()
            setOnDismissListener {
                anchor.isActivated = false
                if (activePopup === this) {
                    activePopup = null
                }
            }
        }

        activePopup = popup
        anchor.isActivated = true
        popup.showAsDropDown(anchor, 0, context.dp(8), Gravity.END)
    }

    private fun calculatePopupWidth(
        context: Context,
        items: List<MoreMenuItem>
    ): Int {
        val textPaint = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }.paint
        val widestText = items.maxOf { item ->
            textPaint.measureText(context.getString(item.titleRes))
        }.toInt()

        return (widestText + context.dp(92)).coerceAtLeast(context.dp(160))
    }

    private fun createMenuItem(
        context: Context,
        item: MoreMenuItem,
        onClick: () -> Unit
    ): View = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.dp(48)
        )
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dp(12), 0, context.dp(16), 0)
        foreground = context.selectableItemBackground()
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }

        addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                context.dp(24),
                context.dp(24)
            )
            setImageResource(item.iconRes)
            imageTintList = AppCompatResources.getColorStateList(
                context,
                R.color.text_primary
            )
        })

        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = context.dp(16)
            }
            setText(item.titleRes)
            maxLines = 1
            setTextColor(
                AppCompatResources.getColorStateList(
                    context,
                    R.color.text_primary
                )
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        })
    }

    private fun Context.dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    private fun Context.selectableItemBackground(): Drawable? {
        val typedValue = TypedValue()
        theme.resolveAttribute(
            android.R.attr.selectableItemBackground,
            typedValue,
            true
        )
        return AppCompatResources.getDrawable(this, typedValue.resourceId)
    }
}
