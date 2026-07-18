package com.autoclicker.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "新任务",
    val createdAt: Long = System.currentTimeMillis()
)
