package com.example.tagmind.ui.itemdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tagmind.TagMindApplication
import com.example.tagmind.data.db.entity.ItemEntity
import com.example.tagmind.data.db.entity.ReminderEntity
import com.example.tagmind.data.db.entity.ScanEventEntity
import com.example.tagmind.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class ItemDetailUiState(
    val item: ItemEntity? = null,
    val activeReminder: ReminderEntity? = null,
    val scanHistory: List<ScanEventEntity> = emptyList(),
    val isLoading: Boolean = true,
)

class ItemDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {
    private val repo = (app as TagMindApplication).repository
    private val scheduler = (app as TagMindApplication).alarmScheduler
    val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.getAllItems(),
                repo.getRemindersForItem(itemId),
                repo.getScanEventsForItem(itemId),
            ) { items, reminders, events ->
                val active = reminders.firstOrNull {
                    it.status == "pending" || it.status == "fired"
                }
                Triple(items.firstOrNull { it.id == itemId }, active, events)
            }.collect { (item, activeReminder, events) ->
                _uiState.value = ItemDetailUiState(
                    item = item,
                    activeReminder = activeReminder,
                    scanHistory = events,
                    isLoading = false,
                )
            }
        }
    }

    fun setReminder(delayMs: Long) {
        viewModelScope.launch {
            val fireAt = System.currentTimeMillis() + delayMs
            val reminder = repo.createReminder(itemId = itemId, fireAt = fireAt)
            scheduler.scheduleAlarm(
                reminderId = reminder.id,
                itemId = itemId,
                notificationId = reminder.notificationId,
                fireAtMs = fireAt,
            )
        }
    }

    fun cancelReminder() {
        viewModelScope.launch {
            val active = _uiState.value.activeReminder ?: return@launch
            scheduler.cancelAlarm(active.notificationId)
            repo.cancelReminder(active.id)
        }
    }

    fun confirmReminder() {
        viewModelScope.launch {
            val active = _uiState.value.activeReminder ?: return@launch
            scheduler.cancelAlarm(active.notificationId)
            repo.confirmReminder(active.id)
            NotificationHelper(getApplication()).cancelNotification(active.notificationId)
        }
    }

    fun editItem(name: String, emoji: String) {
        viewModelScope.launch { repo.updateItem(itemId, name, emoji) }
    }

    /** Deletes the item (reminders and history cascade) and clears any scheduled alert. */
    fun deleteItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.getRemindersForItem(itemId).first().forEach {
                scheduler.cancelAlarm(it.notificationId)
                NotificationHelper(getApplication()).cancelNotification(it.notificationId)
            }
            repo.deleteItem(itemId)
            onDeleted()
        }
    }

    fun onTagScanned(uid: String) {
        if (_uiState.value.item?.nfcUid == uid) confirmReminder()
    }
}
