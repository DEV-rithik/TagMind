package com.example.tagmind.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "scan_events",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class ScanEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val type: String,           // "register" | "remind" | "confirm"
    val timestamp: Long = System.currentTimeMillis(),
)

object ScanType {
    const val REGISTER = "register"
    const val REMIND = "remind"
    const val CONFIRM = "confirm"
}
