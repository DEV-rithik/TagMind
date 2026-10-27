package com.example.tagmind.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tagmind.TagMindApplication
import com.example.tagmind.data.db.entity.ItemEntity
import com.example.tagmind.data.db.entity.ReminderEntity
import com.example.tagmind.data.db.entity.ReminderStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemWithReminder(
    val item: ItemEntity,
    val activeReminder: ReminderEntity?,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val items: List<ItemWithReminder>) : HomeUiState
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as TagMindApplication).repository
    private val scheduler = (app as TagMindApplication).alarmScheduler

    val uiState: StateFlow<HomeUiState> = combine(
        repo.getAllItems(),
        repo.getActiveReminders(),
    ) { items, activeReminders ->
        val reminderMap = activeReminders.associateBy { it.itemId }
        HomeUiState.Success(
            items = items.map { item ->
                ItemWithReminder(item = item, activeReminder = reminderMap[item.id])
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun setReminder(itemId: String, delayMs: Long) {
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

    fun cancelReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            scheduler.cancelAlarm(reminder.notificationId)
            repo.cancelReminder(reminder.id)
        }
    }
}
