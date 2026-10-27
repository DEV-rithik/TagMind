package com.example.tagmind.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tagmind.data.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE status = 'pending' OR status = 'fired' ORDER BY fireAt ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getRemindersForItem(itemId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE itemId = :itemId AND (status = 'pending' OR status = 'fired') LIMIT 1")
    suspend fun getActiveReminderForItem(itemId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'pending'")
    suspend fun getAllPendingReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE reminders SET status = :status, confirmedAt = :confirmedAt WHERE id = :id")
    suspend fun confirmReminder(id: String, status: String, confirmedAt: Long)
}
