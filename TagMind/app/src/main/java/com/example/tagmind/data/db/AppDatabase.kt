package com.example.tagmind.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tagmind.data.db.dao.ItemDao
import com.example.tagmind.data.db.dao.ReminderDao
import com.example.tagmind.data.db.dao.ScanEventDao
import com.example.tagmind.data.db.entity.ItemEntity
import com.example.tagmind.data.db.entity.ReminderEntity
import com.example.tagmind.data.db.entity.ScanEventEntity

@Database(
    entities = [ItemEntity::class, ReminderEntity::class, ScanEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun reminderDao(): ReminderDao
    abstract fun scanEventDao(): ScanEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tagmind_database",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
