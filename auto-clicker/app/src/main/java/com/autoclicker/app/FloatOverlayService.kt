package com.autoclicker.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.autoclicker.app.ClickAccessibilityService.Companion.getInstance
import com.autoclicker.app.ClickAccessibilityService.Companion.isRunning

class FloatOverlayService : Service() {

    companion object {
        private const val TAG = "FloatOverlay"
        private const val CHANNEL_ID = "FloatOverlayChannel"
        private const val NOTIFICATION_ID = 1001

        var isRunning = false
            private set

        private var instance: FloatOverlayService? = null

        fun getInstance(): FloatOverlayService? = instance

        fun start(context: Context) {
            val intent = Intent(context, FloatOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<OverlayButtonView>()
    private var isClickingActive = false
    private var clickCallback: ClickAccessibilityService.ClickTask? = null

    data class OverlayButtonInfo(
        val id: Long,
        val label: String,
        var posX: Float,
        var posY: Float,
        val clickCount: Int,
        val clickDurationMs: Long,
        val clickIntervalMs: Long,
        val orderIndex: Int
    )

    private val buttonInfoList = mutableListOf<OverlayButtonInfo>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAllOverlayButtons()
        isRunning = false
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun addButton(info: OverlayButtonInfo) {
        buttonInfoList.add(info)
        createOverlayButton(info)
    }

    fun removeButton(buttonId: Long) {
        val iterator = overlayViews.iterator()
        while (iterator.hasNext()) {
            val view = iterator.next()
            if (view.buttonId == buttonId) {
                windowManager.removeView(view.rootView)
                iterator.remove()
            }
        }
        buttonInfoList.removeAll { it.id == buttonId }
    }

    fun updateButtonPosition(buttonId: Long, newX: Float, newY: Float) {
        buttonInfoList.find { it.id == buttonId }?.let { info ->
            info.posX = newX
            info.posY = newY
        }
        overlayViews.find { it.buttonId == buttonId }?.let { view ->
            updateOverlayButtonPosition(view, newX, newY)
        }
    }

    fun updateButtonConfig(info: OverlayButtonInfo) {
        val idx = buttonInfoList.indexOfFirst { it.id == info.id }
        if (idx >= 0) {
            buttonInfoList[idx] = info
        }
        overlayViews.find { it.buttonId == info.id }?.let { view ->
            view.labelView.text = info.label
        }
    }

    fun clearAllButtons() {
        removeAllOverlayButtons()
        buttonInfoList.clear()
    }

    fun getAllButtonPositions(): List<OverlayButtonInfo> = buttonInfoList.toList()

    fun startClickingAll() {
        if (!isRunning()) {
            return
        }

        if (buttonInfoList.isEmpty()) return

        val clickTasks = buttonInfoList.map { info ->
            ClickAccessibilityService.ClickTask(
                posX = info.posX,
                posY = info.posY,
                clickCount = info.clickCount,
                clickDurationMs = info.clickDurationMs,
                clickIntervalMs = info.clickIntervalMs
            )
        }

        getInstance()?.startClicking(clickTasks)

        isClickingActive = true
        updateButtonsVisualState()
    }

    fun stopClickingAll() {
        getInstance()?.stopClicking()
        isClickingActive = false
        updateButtonsVisualState()
    }

    fun isClickingActive(): Boolean = isClickingActive

    fun getButtonConfigsForPersistence(): List<ButtonPersistenceData> {
        return buttonInfoList.map { info ->
            ButtonPersistenceData(
                id = info.id,
                label = info.label,
                posX = info.posX,
                posY = info.posY,
                clickCount = info.clickCount,
                clickDurationMs = info.clickDurationMs,
                clickIntervalMs = info.clickIntervalMs,
                orderIndex = info.orderIndex
            )
        }
    }

    data class ButtonPersistenceData(
        val id: Long,
        val label: String,
        val posX: Float,
        val posY: Float,
        val clickCount: Int,
        val clickDurationMs: Long,
        val clickIntervalMs: Long,
        val orderIndex: Int
    )

    private fun createOverlayButton(info: OverlayButtonInfo) {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.x = info.posX.toInt()
        layoutParams.y = info.posY.toInt()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rootView = inflater.inflate(R.layout.overlay_button, null)

        val bubbleView = rootView.findViewById<View>(R.id.bubble)
        val labelView = rootView.findViewById<TextView>(R.id.label)

        labelView.text = info.label

        val params = layoutParams
        val overlayView = OverlayButtonView(
            rootView = rootView,
            layoutParams = params,
            buttonId = info.id,
            labelView = labelView,
            bubbleView = bubbleView
        )

        // Touch listener for dragging
        rootView.setOnTouchListener { _, event ->
            dragHandler(overlayView, event)
            true
        }

        // Long press to configure
        rootView.setOnLongClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("configure_button_id", info.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            true
        }

        windowManager.addView(rootView, layoutParams)
        overlayViews.add(overlayView)
    }

    private fun removeAllOverlayButtons() {
        for (view in overlayViews) {
            try {
                windowManager.removeView(view.rootView)
            } catch (e: Exception) {
                // View already removed
            }
        }
        overlayViews.clear()
    }

    private fun updateOverlayButtonPosition(view: OverlayButtonView, x: Float, y: Float) {
        view.layoutParams.x = x.toInt()
        view.layoutParams.y = y.toInt()
        windowManager.updateViewLayout(view.rootView, view.layoutParams)
    }

    private fun updateButtonsVisualState() {
        for (view in overlayViews) {
            view.bubbleView.alpha = if (isClickingActive) 0.5f else 1.0f
        }
    }

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private fun dragHandler(view: OverlayButtonView, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = view.layoutParams.x.toFloat()
                initialY = view.layoutParams.y.toFloat()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                    isDragging = true
                }

                view.layoutParams.x = (initialX + deltaX).toInt()
                view.layoutParams.y = (initialY + deltaY).toInt()
                windowManager.updateViewLayout(view.rootView, view.layoutParams)

                // Update position in real-time
                val info = buttonInfoList.find { it.id == view.buttonId }
                info?.posX = view.layoutParams.x.toFloat()
                info?.posY = view.layoutParams.y.toFloat()
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Tap - toggle clicking for this button or show info
                }
                // Persist the final position
            }
        }
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮球连点器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "连点器后台运行通知"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("连点器")
            .setContentText("悬浮球正在运行")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private class OverlayButtonView(
        val rootView: View,
        val layoutParams: WindowManager.LayoutParams,
        val buttonId: Long,
        val labelView: TextView,
        val bubbleView: View
    )
}
