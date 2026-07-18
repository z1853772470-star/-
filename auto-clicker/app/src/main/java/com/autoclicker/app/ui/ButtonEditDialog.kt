package com.autoclicker.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.autoclicker.app.data.ButtonConfigEntity

@Composable
fun ButtonEditDialog(
    button: ButtonConfigEntity,
    onDismiss: () -> Unit,
    onSave: (ButtonConfigEntity) -> Unit,
    onDelete: () -> Unit
) {
    var label by remember { mutableStateOf(button.label) }
    var clickCount by remember { mutableStateOf(button.clickCount.toString()) }
    var clickDuration by remember { mutableStateOf(button.clickDurationMs.toString()) }
    var clickInterval by remember { mutableStateOf(button.clickIntervalMs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑按钮 - $label") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("按钮名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = clickCount,
                    onValueChange = { clickCount = it.filter { c -> c.isDigit() } },
                    label = { Text("点击次数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = clickDuration,
                    onValueChange = { clickDuration = it.filter { c -> c.isDigit() } },
                    label = { Text("每次点击时长 (毫秒)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("每次按下屏幕持续的时间") }
                )

                OutlinedTextField(
                    value = clickInterval,
                    onValueChange = { clickInterval = it.filter { c -> c.isDigit() } },
                    label = { Text("点击间隔 (毫秒)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("两次点击之间的等待时间") }
                )

                // Show current position info
                Text(
                    text = "位置: (${button.posX.toInt()}, ${button.posY.toInt()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "提示: 在悬浮球模式下可直接拖动按钮调整位置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除按钮")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val updated = button.copy(
                            label = label.ifBlank { "按钮" },
                            clickCount = clickCount.toIntOrNull() ?: button.clickCount,
                            clickDurationMs = clickDuration.toLongOrNull() ?: button.clickDurationMs,
                            clickIntervalMs = clickInterval.toLongOrNull() ?: button.clickIntervalMs
                        )
                        onSave(updated)
                    }
                ) {
                    Text("保存")
                }
            }
        }
    )
}
