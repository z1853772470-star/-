package com.autoclicker.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    // Button configs
    @Query("SELECT * FROM button_configs WHERE taskId = :taskId ORDER BY orderIndex ASC")
    fun getButtonsForTask(taskId: Long): Flow<List<ButtonConfigEntity>>

    @Query("SELECT * FROM button_configs WHERE taskId = :taskId ORDER BY orderIndex ASC")
    suspend fun getButtonsForTaskSync(taskId: Long): List<ButtonConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: ButtonConfigEntity): Long

    @Update
    suspend fun updateButton(button: ButtonConfigEntity)

    @Delete
    suspend fun deleteButton(button: ButtonConfigEntity)

    @Query("DELETE FROM button_configs WHERE id = :buttonId")
    suspend fun deleteButtonById(buttonId: Long)

    @Query("DELETE FROM button_configs WHERE taskId = :taskId")
    suspend fun deleteButtonsForTask(taskId: Long)
}
