package com.example.tagmind.data

import com.example.tagmind.data.db.dao.*
import com.example.tagmind.data.db.entity.*
import com.example.tagmind.data.repository.TagMindRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TagMindRepositoryTest {
    private val items = FakeItems()
    private val reminders = FakeReminders()
    private val scans = FakeScans()
    private val repo = TagMindRepository(items, reminders, scans)

    @Test fun registrationStoresUidAndHistory() = runTest {
        val item = repo.registerItem("04AB12", "Helmet", "H")
        assertEquals(item, repo.getItemByNfcUid("04AB12"))
        assertEquals(item.id, scans.rows.single().itemId)
        assertEquals(ScanType.REGISTER, scans.rows.single().type)
    }

    @Test fun replacementCancelsPreviousReminder() = runTest {
        val first = repo.createReminder("item", 1000L)
        val second = repo.createReminder("item", 2000L)
        assertEquals(ReminderStatus.CANCELLED, repo.getReminderById(first.id)?.status)
        assertEquals(second, repo.getActiveReminderForItem("item"))
    }

    @Test fun confirmationRecordsTimeAndScan() = runTest {
        val reminder = repo.createReminder("item", 1000L)
        repo.markReminderFired(reminder.id)
        repo.confirmReminder(reminder.id)
        val confirmed = repo.getReminderById(reminder.id)!!
        assertEquals(ReminderStatus.CONFIRMED, confirmed.status)
        assertNotNull(confirmed.confirmedAt)
        assertNull(repo.getActiveReminderForItem("item"))
        assertEquals(ScanType.CONFIRM, scans.rows.last().type)
        assertEquals("item", scans.rows.last().itemId)
    }
}

private class FakeItems : ItemDao {
    val rows = mutableMapOf<String, ItemEntity>()
    override fun getAllItems() = flowOf(rows.values.toList())
    override suspend fun getItemById(id: String) = rows[id]
    override suspend fun getItemByNfcUid(nfcUid: String) = rows.values.find { it.nfcUid == nfcUid }
    override suspend fun insertItem(item: ItemEntity) { rows[item.id] = item }
    override suspend fun updateItem(item: ItemEntity) { rows[item.id] = item }
    override suspend fun deleteItem(id: String) { rows.remove(id) }
}

private class FakeReminders : ReminderDao {
    val rows = mutableMapOf<String, ReminderEntity>()
    private fun active(r: ReminderEntity) = r.status == ReminderStatus.PENDING || r.status == ReminderStatus.FIRED
    override fun getActiveReminders() = flowOf(rows.values.filter(::active))
    override fun getRemindersForItem(itemId: String) = flowOf(rows.values.filter { it.itemId == itemId })
    override suspend fun getReminderById(id: String) = rows[id]
    override suspend fun getActiveReminderForItem(itemId: String) = rows.values.find { it.itemId == itemId && active(it) }
    override suspend fun getAllPendingReminders() = rows.values.filter { it.status == ReminderStatus.PENDING }
    override suspend fun insertReminder(reminder: ReminderEntity) { rows[reminder.id] = reminder }
    override suspend fun updateReminder(reminder: ReminderEntity) { rows[reminder.id] = reminder }
    override suspend fun updateStatus(id: String, status: String) { rows[id]?.let { rows[id] = it.copy(status = status) } }
    override suspend fun confirmReminder(id: String, status: String, confirmedAt: Long) {
        rows[id]?.let { rows[id] = it.copy(status = status, confirmedAt = confirmedAt) }
    }
}

private class FakeScans : ScanEventDao {
    val rows = mutableListOf<ScanEventEntity>()
    override fun getScanEventsForItem(itemId: String) = flowOf(rows.filter { it.itemId == itemId })
    override fun getRecentScanEvents(limit: Int) = flowOf(rows.takeLast(limit))
    override suspend fun insertScanEvent(event: ScanEventEntity) { rows.add(event) }
}
