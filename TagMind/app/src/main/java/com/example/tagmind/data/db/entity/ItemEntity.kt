package com.example.tagmind.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "items",
    indices = [Index(value = ["nfcUid"], unique = true)]
)
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nfcUid: String,       // NFC tag UID hex string, unique
    val name: String,
    val iconEmoji: String,    // e.g. "🏍️"
    val createdAt: Long = System.currentTimeMillis(),
)
