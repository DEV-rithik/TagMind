package com.example.tagmind.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "reminders",
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
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val fireAt: Long,            // epoch ms UTC
    val status: String,          // "pending" | "fired" | "confirmed" | "cancelled"
    val notificationId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = null,
)

object ReminderStatus {
    const val PENDING = "pending"
    const val FIRED = "fired"
    const val CONFIRMED = "confirmed"
    const val CANCELLED = "cancelled"
}
