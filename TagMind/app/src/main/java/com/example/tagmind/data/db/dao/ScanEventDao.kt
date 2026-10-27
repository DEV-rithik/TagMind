package com.example.tagmind.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tagmind.data.db.entity.ScanEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanEventDao {

    @Query("SELECT * FROM scan_events WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getScanEventsForItem(itemId: String): Flow<List<ScanEventEntity>>

    @Query("SELECT * FROM scan_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScanEvents(limit: Int = 20): Flow<List<ScanEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanEvent(event: ScanEventEntity)
}
