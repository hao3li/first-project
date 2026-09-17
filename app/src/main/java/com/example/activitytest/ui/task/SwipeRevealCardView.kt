package com.example.activitytest.ui.task

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs


class SwipeRevealCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    // 展开时露出的操作按钮宽度（144dp）
    private var actionWidthPx: Float = 144f * resources.displayMetrics.density

    // 手势起点与位移记录
    private var downX = 0f
    private var downY = 0f
    private var startOffset = 0f
    private var isSwiping = false

    // 当前是否处于展开（露出按钮）状态
    var isRevealed = false
        private set

    private val defaultShapeAppearanceModel = shapeAppearanceModel
    private val swipedShapeAppearanceModel by lazy {  // 左滑时，去掉右上角和右下角的圆角
        defaultShapeAppearanceModel.toBuilder()  // 使用 toBuilder 创建一个副本，避免影响到原始对象
            .setTopRightCornerSize(0f)
            .setBottomRightCornerSize(0f)
            .build()
    }

    init {
        isClickable = true
    }

    private fun updateCardShape(offset: Float) {
        shapeAppearanceModel = if (offset < 0f) {
            swipedShapeAppearanceModel
        } else {
            defaultShapeAppearanceModel
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                startOffset = translationX
                isSwiping = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY

                // 横向位移明显大于纵向时判定为左滑，拦截事件
                if (!isSwiping && abs(dx) > abs(dy) * 1.5f && abs(dx) > 24f) {
                    isSwiping = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }

                if (isSwiping) {
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isSwiping = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (isSwiping) {
                    val dx = event.rawX - downX
                    val offset = (startOffset + dx)
                        .coerceIn(-actionWidthPx, 0f)
                    translationX = offset
                    updateCardShape(offset)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSwiping) {
                    snapToNearest()
                    isSwiping = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // 手指抬起后吸附到最近的端点：超过一半则完全展开，否则收起
    private fun snapToNearest() {
        val half = actionWidthPx / 2f
        if (translationX <= -half) {
            reveal()
        } else {
            collapse()
        }
    }

    // 展开，露出底部操作按钮
    fun reveal() {
        isRevealed = true
        updateCardShape(-actionWidthPx)
        animate()
            .translationX(-actionWidthPx)
            .setDuration(180L)
            .start()
    }

    // 收起，还原到初始位置
    fun collapse() {
        isRevealed = false
        updateCardShape(0f)
        animate()
            .translationX(0f)
            .setDuration(180L)
            .start()
    }
}
