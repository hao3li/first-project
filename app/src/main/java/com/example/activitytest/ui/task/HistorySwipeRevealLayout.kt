package com.example.activitytest.ui.task

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.example.activitytest.R
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs


class HistorySwipeRevealLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var downX = 0f
    private var downY = 0f
    private var startOffset = 0f
    private var swiping = false
    private var foreground: MaterialCardView? = null
    private var selectionPanel: MaterialCardView? = null
    private var checkbox: View? = null
    private val revealWidth = (56 * resources.displayMetrics.density)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // 记录默认圆角形状，用于滑动过程中动态切换前景卡片左侧的圆角
    private var foregroundDefaultShape: com.google.android.material.shape.ShapeAppearanceModel? = null
    private var foregroundFlatShape: com.google.android.material.shape.ShapeAppearanceModel? = null

    // 当前是否处于展开（露出选择框）状态
    var isRevealed = false
        private set

    // 展开状态发生变化时回调，用于让适配器记住每一条历史任务的展开状态
    var onRevealStateChanged: ((Boolean) -> Unit)? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        foreground = findViewById(R.id.historyForeground)
        selectionPanel = findViewById(R.id.historySelectionPanel)
        checkbox = findViewById(R.id.checkHistorySelected)

        foreground?.let { card ->
            foregroundDefaultShape = card.shapeAppearanceModel
            // 前景卡片左侧（与选择面板相接的一侧）滑开后变为直角
            foregroundFlatShape = card.shapeAppearanceModel.toBuilder()
                .setTopLeftCornerSize(0f)
                .setBottomLeftCornerSize(0f)
                .build()
        }
        selectionPanel?.let { card ->
            // 选择面板右侧（与前景卡片相接的一侧）始终为直角
            val flatShape = card.shapeAppearanceModel.toBuilder()
                .setTopRightCornerSize(0f)
                .setBottomRightCornerSize(0f)
                .build()
            card.shapeAppearanceModel = flatShape
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startOffset = foreground?.translationX ?: 0f
                swiping = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!swiping && abs(dx) > abs(dy) * 1.3f && abs(dx) > touchSlop) {
                    swiping = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                if (swiping) return true
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val card = foreground ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (swiping) {
                    val offset = (startOffset + event.rawX - downX)
                        .coerceIn(0f, revealWidth)
                    card.translationX = offset
                    updateSelectionAlpha(offset)
                    updateForegroundShape(offset)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (swiping) {
                    if (card.translationX >= revealWidth / 2f) reveal() else collapse()
                    swiping = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateSelectionAlpha(offset: Float) {
        checkbox?.alpha = (offset / revealWidth).coerceIn(0f, 1f)
    }

    // 滑开后前景卡片左侧变为直角，与底层选择面板的右侧直角相接；
    // 完全收起时恢复默认圆角
    private fun updateForegroundShape(offset: Float) {
        foreground?.shapeAppearanceModel = if (offset > 0f) {
            foregroundFlatShape ?: return
        } else {
            foregroundDefaultShape ?: return
        }
    }

    // 手势结束时带动画展开，并通知外部状态变化
    fun reveal() {
        val wasRevealed = isRevealed
        isRevealed = true
        foreground?.animate()?.translationX(revealWidth)?.setDuration(160L)?.start()
        checkbox?.animate()?.alpha(1f)?.setDuration(160L)?.start()
        updateForegroundShape(revealWidth)
        if (!wasRevealed) onRevealStateChanged?.invoke(true)
    }

    // 手势结束时带动画收起，并通知外部状态变化
    fun collapse() {
        val wasRevealed = isRevealed
        isRevealed = false
        foreground?.animate()?.translationX(0f)?.setDuration(160L)?.start()
        checkbox?.animate()?.alpha(0f)?.setDuration(160L)?.start()
        updateForegroundShape(0f)
        if (wasRevealed) onRevealStateChanged?.invoke(false)
    }


    fun setRevealedImmediate(revealed: Boolean) {
        isRevealed = revealed
        val offset = if (revealed) revealWidth else 0f
        foreground?.translationX = offset
        checkbox?.alpha = if (revealed) 1f else 0f
        updateForegroundShape(offset)
    }
}
