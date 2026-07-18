package com.autoclicker.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoclicker.app.ClickAccessibilityService
import com.autoclicker.app.FloatOverlayService
import com.autoclicker.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TaskDatabase.getDatabase(application)
    private val taskDao = db.taskDao()

    val allTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    val selectedTaskId: StateFlow<Long?> = _selectedTaskId.asStateFlow()

    private val _buttonsForSelectedTask = MutableStateFlow<List<ButtonConfigEntity>>(emptyList())
    val buttonsForSelectedTask: StateFlow<List<ButtonConfigEntity>> = _buttonsForSelectedTask.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _overlayButtons = MutableStateFlow<List<FloatOverlayService.OverlayButtonInfo>>(emptyList())
    val overlayButtons: StateFlow<List<FloatOverlayService.OverlayButtonInfo>> = _overlayButtons.asStateFlow()

    fun selectTask(taskId: Long?) {
        _selectedTaskId.value = taskId
        if (taskId != null) {
            viewModelScope.launch {
                taskDao.getButtonsForTask(taskId).collect { buttons ->
                    _buttonsForSelectedTask.value = buttons
                }
            }
        } else {
            _buttonsForSelectedTask.value = emptyList()
        }
    }

    fun createTask(name: String) {
        viewModelScope.launch {
            val taskId = taskDao.insertTask(TaskEntity(name = name))
            selectTask(taskId)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            if (_selectedTaskId.value == task.id) {
                selectTask(null)
            }
        }
    }

    fun addButtonToTask(taskId: Long, label: String = "按钮") {
        viewModelScope.launch {
            val buttons = taskDao.getButtonsForTaskSync(taskId)
            val newOrder = buttons.size
            // Start button at a reasonable position
            val baseX = 200f + (newOrder % 4) * 180f
            val baseY = 400f + (newOrder / 4) * 180f
            val button = ButtonConfigEntity(
                taskId = taskId,
                label = "$label${newOrder + 1}",
                posX = baseX,
                posY = baseY,
                clickCount = 10,
                clickDurationMs = 50,
                clickIntervalMs = 200,
                orderIndex = newOrder
            )
            taskDao.insertButton(button)
        }
    }

    fun updateButtonConfig(button: ButtonConfigEntity) {
        viewModelScope.launch {
            taskDao.updateButton(button)
            // Also update on overlay if active
            val service = FloatOverlayService.getInstance()
            if (service != null && service.isRunning) {
                service.updateButtonConfig(
                    FloatOverlayService.OverlayButtonInfo(
                        id = button.id,
                        label = button.label,
                        posX = button.posX,
                        posY = button.posY,
                        clickCount = button.clickCount,
                        clickDurationMs = button.clickDurationMs,
                        clickIntervalMs = button.clickIntervalMs,
                        orderIndex = button.orderIndex
                    )
                )
            }
        }
    }

    fun deleteButton(button: ButtonConfigEntity) {
        viewModelScope.launch {
            taskDao.deleteButton(button)
            val service = FloatOverlayService.getInstance()
            service?.removeButton(button.id)
        }
    }

    fun updateButtonPosition(buttonId: Long, newX: Float, newY: Float) {
        viewModelScope.launch {
            val current = _buttonsForSelectedTask.value.find { it.id == buttonId }
            if (current != null) {
                taskDao.updateButton(current.copy(posX = newX, posY = newY))
            }
        }
    }

    fun showButtonsOnOverlay(taskId: Long) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val buttons = taskDao.getButtonsForTaskSync(taskId)
            val service = FloatOverlayService.getInstance()
            if (service != null && service.isRunning) {
                service.clearAllButtons()
                buttons.forEach { button ->
                    service.addButton(
                        FloatOverlayService.OverlayButtonInfo(
                            id = button.id,
                            label = button.label,
                            posX = button.posX,
                            posY = button.posY,
                            clickCount = button.clickCount,
                            clickDurationMs = button.clickDurationMs,
                            clickIntervalMs = button.clickIntervalMs,
                            orderIndex = button.orderIndex
                        )
                    )
                }
                refreshOverlayButtons()
            } else {
                // Request overlay permission and start service
                _pendingTaskToShow = taskId
            }
        }
    }

    private var _pendingTaskToShow: Long? = null

    fun startOverlayService() {
        val app = getApplication<Application>()
        FloatOverlayService.start(app)
        _isOverlayActive.value = true

        _pendingTaskToShow?.let { taskId ->
            _pendingTaskToShow = null
            showButtonsOnOverlay(taskId)
        }
    }

    fun stopOverlayService() {
        val app = getApplication<Application>()
        FloatOverlayService.getInstance()?.clearAllButtons()
        FloatOverlayService.stop(app)
        _isOverlayActive.value = false
        refreshOverlayButtons()
    }

    fun startClicking() {
        FloatOverlayService.getInstance()?.startClickingAll()
    }

    fun stopClicking() {
        FloatOverlayService.getInstance()?.stopClickingAll()
    }

    fun refreshOverlayButtons() {
        val service = FloatOverlayService.getInstance()
        if (service != null && service.isRunning) {
            _overlayButtons.value = service.getAllButtonPositions()
            _isOverlayActive.value = true
        } else {
            _overlayButtons.value = emptyList()
            _isOverlayActive.value = false
        }
    }

    fun syncPositionsFromOverlay() {
        val service = FloatOverlayService.getInstance() ?: return
        val positions = service.getButtonConfigsForPersistence()
        viewModelScope.launch {
            for (pos in positions) {
                taskDao.updateButton(
                    ButtonConfigEntity(
                        id = pos.id,
                        taskId = _selectedTaskId.value ?: 0,
                        label = pos.label,
                        posX = pos.posX,
                        posY = pos.posY,
                        clickCount = pos.clickCount,
                        clickDurationMs = pos.clickDurationMs,
                        clickIntervalMs = pos.clickIntervalMs,
                        orderIndex = pos.orderIndex
                    )
                )
            }
        }
    }
}
