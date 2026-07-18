package com.autoclicker.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "button_configs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class ButtonConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long = 0,
    val label: String = "按钮",
    val posX: Float = 500f,
    val posY: Float = 500f,
    val clickCount: Int = 10,
    val clickDurationMs: Long = 50,
    val clickIntervalMs: Long = 100,
    val orderIndex: Int = 0
)
