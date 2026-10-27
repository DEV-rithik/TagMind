package com.example.tagmind.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tagmind.data.db.AppDatabase
import com.example.tagmind.data.db.entity.ReminderStatus
import com.example.tagmind.data.db.entity.ScanEventEntity
import com.example.tagmind.data.db.entity.ScanType
import com.example.tagmind.notification.NotificationHelper
import com.example.tagmind.widget.TagMindWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that fires when a reminder alarm goes off.
 * Posts a notification and updates the reminder status to "fired".
 * Also handles the "I have it" action tapped on that notification.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_REMINDER_FIRED && action != ACTION_CONFIRM) return

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        val db = AppDatabase.getInstance(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch

                if (action == ACTION_CONFIRM) {
                    if (reminder.status == ReminderStatus.PENDING || reminder.status == ReminderStatus.FIRED) {
                        db.reminderDao().confirmReminder(
                            id = reminderId,
                            status = ReminderStatus.CONFIRMED,
                            confirmedAt = System.currentTimeMillis(),
                        )
                        db.scanEventDao().insertScanEvent(
                            ScanEventEntity(itemId = reminder.itemId, type = ScanType.CONFIRM)
                        )
                    }
                    NotificationHelper(context).cancelNotification(notificationId)
                    return@launch
                }

                if (reminder.status != ReminderStatus.PENDING || reminder.itemId != itemId) return@launch
                // Update reminder status to "fired"
                db.reminderDao().updateStatus(reminderId, ReminderStatus.FIRED)

                // Get item info for the notification
                val item = db.itemDao().getItemById(itemId)
                val itemName = item?.name ?: "your item"
                val itemEmoji = item?.iconEmoji ?: "📦"

                // Post notification
                NotificationHelper(context).showReminderNotification(
                    notificationId = notificationId,
                    itemName = itemName,
                    itemEmoji = itemEmoji,
                    reminderId = reminderId,
                    itemId = itemId,
                )
            } finally {
                TagMindWidgetProvider.refreshNow(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER_FIRED = "com.example.tagmind.ACTION_REMINDER_FIRED"
        const val ACTION_CONFIRM = "com.example.tagmind.ACTION_CONFIRM_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
