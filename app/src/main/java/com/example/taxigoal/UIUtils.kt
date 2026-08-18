package com.example.taxigoal

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object UIUtils {
    /**
     * Создает эффект "пружинного" нажатия для любой View.
     */
    fun setSpringClick(view: View, onClick: () -> Unit) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction { onClick() }
                        .start()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
            }
            true
        }
    }

    /**
     * Плавное появление View снизу вверх.
     */
    fun animateIn(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationY = 100f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}
