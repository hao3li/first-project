package com.example.activitytest.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout

class KeyboardDismissLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var touchDownY = 0f
    private var keyboardDismissed = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        isFocusableInTouchMode = true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = event.rawY
                keyboardDismissed = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!keyboardDismissed && event.rawY - touchDownY > touchSlop) {
                    dismissKeyboard()
                    keyboardDismissed = true
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun dismissKeyboard() {
        val focusedView = findFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(focusedView?.windowToken ?: windowToken, 0)
        focusedView?.clearFocus()
        requestFocus()
    }
}
