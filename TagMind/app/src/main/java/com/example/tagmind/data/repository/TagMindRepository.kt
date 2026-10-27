package com.example.tagmind.data.repository

import com.example.tagmind.data.db.dao.ItemDao
import com.example.tagmind.data.db.dao.ReminderDao
import com.example.tagmind.data.db.dao.ScanEventDao
import com.example.tagmind.data.db.entity.ItemEntity
import com.example.tagmind.data.db.entity.ReminderEntity
import com.example.tagmind.data.db.entity.ReminderStatus
import com.example.tagmind.data.db.entity.ScanEventEntity
import com.example.tagmind.data.db.entity.ScanType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TagMindRepository(
    private val itemDao: ItemDao,
    private val reminderDao: ReminderDao,
    private val scanEventDao: ScanEventDao,
) {
    // ── Items ──────────────────────────────────────────────────
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: String): ItemEntity? = itemDao.getItemById(id)

    suspend fun getItemByNfcUid(nfcUid: String): ItemEntity? = itemDao.getItemByNfcUid(nfcUid)

    suspend fun registerItem(nfcUid: String, name: String, iconEmoji: String): ItemEntity {
        val item = ItemEntity(
            nfcUid = nfcUid,
            name = name,
            iconEmoji = iconEmoji,
        )
        itemDao.insertItem(item)
        scanEventDao.insertScanEvent(
            ScanEventEntity(itemId = item.id, type = ScanType.REGISTER)
        )
        return item
    }

    suspend fun updateItem(id: String, name: String, iconEmoji: String) {
        val existing = itemDao.getItemById(id) ?: return
        itemDao.updateItem(existing.copy(name = name, iconEmoji = iconEmoji))
    }

    suspend fun deleteItem(id: String) = itemDao.deleteItem(id)

    // ── Reminders ──────────────────────────────────────────────
    fun getActiveReminders(): Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    fun getRemindersForItem(itemId: String): Flow<List<ReminderEntity>> =
        reminderDao.getRemindersForItem(itemId)

    suspend fun getActiveReminderForItem(itemId: String): ReminderEntity? =
        reminderDao.getActiveReminderForItem(itemId)

    suspend fun getAllPendingReminders(): List<ReminderEntity> =
        reminderDao.getAllPendingReminders()

    suspend fun createReminder(itemId: String, fireAt: Long): ReminderEntity {
        // Cancel any existing active reminder for this item
        val existing = reminderDao.getActiveReminderForItem(itemId)
        if (existing != null) {
            reminderDao.updateStatus(existing.id, ReminderStatus.CANCELLED)
        }

        val reminder = ReminderEntity(
            itemId = itemId,
            fireAt = fireAt,
            status = ReminderStatus.PENDING,
            notificationId = UUID.randomUUID().hashCode() and Int.MAX_VALUE,
        )
        reminderDao.insertReminder(reminder)
        scanEventDao.insertScanEvent(
            ScanEventEntity(itemId = itemId, type = ScanType.REMIND)
        )
        return reminder
    }

    suspend fun markReminderFired(reminderId: String) {
        reminderDao.updateStatus(reminderId, ReminderStatus.FIRED)
    }

    suspend fun confirmReminder(reminderId: String) {
        reminderDao.confirmReminder(
            id = reminderId,
            status = ReminderStatus.CONFIRMED,
            confirmedAt = System.currentTimeMillis(),
        )
        val reminder = reminderDao.getReminderById(reminderId)
        if (reminder != null) {
            scanEventDao.insertScanEvent(
                ScanEventEntity(itemId = reminder.itemId, type = ScanType.CONFIRM)
            )
        }
    }

    suspend fun cancelReminder(reminderId: String) {
        reminderDao.updateStatus(reminderId, ReminderStatus.CANCELLED)
    }

    suspend fun getReminderById(id: String): ReminderEntity? = reminderDao.getReminderById(id)

    // ── Scan Events ────────────────────────────────────────────
    fun getScanEventsForItem(itemId: String): Flow<List<ScanEventEntity>> =
        scanEventDao.getScanEventsForItem(itemId)

    fun getRecentScanEvents(limit: Int = 20): Flow<List<ScanEventEntity>> =
        scanEventDao.getRecentScanEvents(limit)
}
