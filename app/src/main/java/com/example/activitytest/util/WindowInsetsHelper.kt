package com.example.activitytest.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object WindowInsetsHelper {


    fun applySystemBarPadding(
        view: View,
        applyTop: Boolean = true,
        applyBottom: Boolean = true
    ) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.setPadding(
                initialLeft + systemBars.left,
                initialTop + (if (applyTop) systemBars.top else 0),
                initialRight + systemBars.right,
                initialBottom + (if (applyBottom) systemBars.bottom else 0)
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}
