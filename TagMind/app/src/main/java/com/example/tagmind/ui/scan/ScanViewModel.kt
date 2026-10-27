package com.example.tagmind.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tagmind.TagMindApplication
import com.example.tagmind.data.db.entity.ItemEntity
import com.example.tagmind.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Scanning : ScanUiState                       // Waiting for NFC tap
    data class UnknownTag(val uid: String) : ScanUiState    // Tag not registered — show Register sheet
    data class KnownItem(val item: ItemEntity) : ScanUiState // Tag recognized
    data class Registered(val item: ItemEntity) : ScanUiState // Just registered
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TagMindApplication).repository
    private val scheduler = (app as TagMindApplication).alarmScheduler

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Scanning)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onTagScanned(uid: String) {
        viewModelScope.launch {
            val existing = repo.getItemByNfcUid(uid)
            _uiState.value = if (existing != null) {
                repo.getActiveReminderForItem(existing.id)?.let { reminder ->
                    scheduler.cancelAlarm(reminder.notificationId)
                    repo.confirmReminder(reminder.id)
                    NotificationHelper(getApplication()).cancelNotification(reminder.notificationId)
                }
                ScanUiState.KnownItem(existing)
            } else {
                ScanUiState.UnknownTag(uid)
            }
        }
    }

    fun registerItem(uid: String, name: String, emoji: String, setReminderDelayMs: Long?) {
        viewModelScope.launch {
            try {
                val item = repo.registerItem(nfcUid = uid, name = name, iconEmoji = emoji)
                if (setReminderDelayMs != null) {
                    val fireAt = System.currentTimeMillis() + setReminderDelayMs
                    val reminder = repo.createReminder(itemId = item.id, fireAt = fireAt)
                    scheduler.scheduleAlarm(
                        reminderId = reminder.id,
                        itemId = item.id,
                        notificationId = reminder.notificationId,
                        fireAtMs = fireAt,
                    )
                }
                _uiState.value = ScanUiState.Registered(item)
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Failed to register")
            }
        }
    }

    fun resetToScanning() {
        _uiState.value = ScanUiState.Scanning
    }
}
