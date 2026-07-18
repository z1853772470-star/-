package com.autoclicker.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoclicker.app.FloatOverlayService
import com.autoclicker.app.data.ButtonConfigEntity
import com.autoclicker.app.data.TaskEntity
import com.autoclicker.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val selectedTaskId by viewModel.selectedTaskId.collectAsState()
    val buttons by viewModel.buttonsForSelectedTask.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val overlayButtonInfos by viewModel.overlayButtons.collectAsState()

    var showNewTaskDialog by remember { mutableStateOf(false) }
    var showButtonEditDialog by remember { mutableStateOf<ButtonConfigEntity?>(null) }
    var showOverlayButtons by remember { mutableStateOf(false) }

    val isServiceRunning = isOverlayActive && FloatOverlayService.getInstance()?.isRunning == true
    val isClickingActive = FloatOverlayService.getInstance()?.isClickingActive() ?: false

    // Refresh overlay state periodically
    LaunchedEffect(Unit) {
        viewModel.refreshOverlayButtons()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "连点器",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (isServiceRunning) {
                        IconButton(onClick = { showOverlayButtons = !showOverlayButtons }) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "显示悬浮按钮",
                                tint = if (showOverlayButtons)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            if (isClickingActive) {
                                viewModel.stopClicking()
                            } else {
                                viewModel.startClicking()
                            }
                        }) {
                            Icon(
                                if (isClickingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isClickingActive) "停止连点" else "开始连点",
                                tint = if (isClickingActive)
                                    Color.Red
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = {
                        if (isServiceRunning) {
                            viewModel.stopOverlayService()
                        } else {
                            // Request overlay permission
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } else {
                                    viewModel.startOverlayService()
                                }
                            } else {
                                viewModel.startOverlayService()
                            }
                        }
                    }) {
                        Icon(
                            if (isServiceRunning) Icons.Default.Album else Icons.Default.Adjust,
                            contentDescription = if (isServiceRunning) "关闭悬浮球" else "开启悬浮球",
                            tint = if (isServiceRunning)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新建任务",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Service status indicator
            if (isServiceRunning) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isClickingActive) Color.Red else Color.Green
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isClickingActive) "连点中..." else "悬浮球运行中",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (isClickingActive) {
                            TextButton(onClick = { viewModel.stopClicking() }) {
                                Text("停止", color = Color.Red)
                            }
                        } else {
                            TextButton(onClick = { viewModel.startClicking() }) {
                                Text("开始连点")
                            }
                        }
                    }
                }
            }

            // Task List Header
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂无任务，点击右下角 + 新建任务",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isSelected = selectedTaskId == task.id,
                            onClick = { viewModel.selectTask(task.id) },
                            onShowOnOverlay = { viewModel.showButtonsOnOverlay(task.id) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }

                HorizontalDivider()

                // Button configuration area
                if (selectedTaskId != null) {
                    ButtonConfigArea(
                        buttons = buttons,
                        selectedTaskId = selectedTaskId!!,
                        viewModel = viewModel,
                        onEditButton = { showButtonEditDialog = it }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "选择一个任务来配置点击按钮",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showNewTaskDialog) {
        TaskEditDialog(
            onDismiss = { showNewTaskDialog = false },
            onConfirm = { name ->
                viewModel.createTask(name)
                showNewTaskDialog = false
            }
        )
    }

    showButtonEditDialog?.let { button ->
        ButtonEditDialog(
            button = button,
            onDismiss = { showButtonEditDialog = null },
            onSave = { updated ->
                viewModel.updateButtonConfig(updated)
                showButtonEditDialog = null
            },
            onDelete = {
                viewModel.deleteButton(button)
                showButtonEditDialog = null
            }
        )
    }

    // Overlay button preview panel
    if (showOverlayButtons && isServiceRunning) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "悬浮按钮列表 (${overlayButtonInfos.size}个)",
                    style = MaterialTheme.typography.titleSmall
                )
                LazyColumn {
                    items(overlayButtonInfos) { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    info.label.take(2),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(info.label, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "点击${info.clickCount}次 | 间隔${info.clickIntervalMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "(${info.posX.toInt()}, ${info.posY.toInt()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onShowOnOverlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onShowOnOverlay) {
                Icon(
                    Icons.Default.NearMe,
                    contentDescription = "显示到悬浮球",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除任务",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ButtonConfigArea(
    buttons: List<ButtonConfigEntity>,
    selectedTaskId: Long,
    viewModel: MainViewModel,
    onEditButton: (ButtonConfigEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "点击按钮 (${buttons.size}个)",
                style = MaterialTheme.typography.titleSmall
            )
            Button(
                onClick = { viewModel.addButtonToTask(selectedTaskId) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加按钮",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加按钮")
            }
        }

        if (buttons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "点击"添加按钮"来添加可拖动的点击位置",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(buttons, key = { it.id }) { button ->
                    ButtonConfigCard(
                        button = button,
                        onClick = { onEditButton(button) },
                        onDelete = { viewModel.deleteButton(button) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonConfigCard(
    button: ButtonConfigEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    button.label.take(2),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    button.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "点击${button.clickCount}次 | 时长${button.clickDurationMs}ms | 间隔${button.clickIntervalMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "位置: (${button.posX.toInt()}, ${button.posY.toInt()})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
