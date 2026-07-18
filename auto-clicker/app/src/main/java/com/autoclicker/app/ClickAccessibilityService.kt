package com.autoclicker.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ClickAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ClickAccessibility"
        private var instance: ClickAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        fun getInstance(): ClickAccessibilityService? = instance
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isClicking = false
    private var clickTasks = mutableListOf<ClickTask>()
    private var currentTaskIndex = 0
    private var currentClickInTask = 0
    private var isPausedForInterval = false

    data class ClickTask(
        val posX: Float,
        val posY: Float,
        val clickCount: Int,
        val clickDurationMs: Long,
        val clickIntervalMs: Long
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopClicking()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClicking()
        instance = null
    }

    fun startClicking(tasks: List<ClickTask>) {
        stopClicking()
        clickTasks = tasks.toMutableList()
        isClicking = true
        currentTaskIndex = 0
        currentClickInTask = 0
        executeNextClick()
    }

    fun stopClicking() {
        isClicking = false
        handler.removeCallbacksAndMessages(null)
        clickTasks.clear()
        currentTaskIndex = 0
        currentClickInTask = 0
        isPausedForInterval = false
    }

    fun isCurrentlyClicking(): Boolean = isClicking

    private fun executeNextClick() {
        if (!isClicking || clickTasks.isEmpty()) {
            stopClicking()
            return
        }

        if (currentTaskIndex >= clickTasks.size) {
            // All tasks completed, restart from beginning
            currentTaskIndex = 0
            currentClickInTask = 0
            if (isPausedForInterval) {
                isPausedForInterval = false
            }
            // Loop back through all tasks
            handler.postDelayed({ executeNextClick() }, 50)
            return
        }

        val task = clickTasks[currentTaskIndex]

        if (currentClickInTask >= task.clickCount) {
            // Move to next button
            currentTaskIndex++
            currentClickInTask = 0
            handler.postDelayed({ executeNextClick() }, task.clickIntervalMs)
            return
        }

        // Perform the click
        performClick(task.posX, task.posY, task.clickDurationMs)
        currentClickInTask++

        // Schedule next click
        handler.postDelayed({ executeNextClick() }, task.clickIntervalMs)
    }

    private fun performClick(x: Float, y: Float, durationMs: Long) {
        val path = Path()
        path.moveTo(x, y)

        val clickDuration = if (durationMs <= 0) 1 else durationMs

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, clickDuration))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
