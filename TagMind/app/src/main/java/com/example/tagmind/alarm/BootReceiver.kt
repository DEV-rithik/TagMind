package com.example.tagmind.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tagmind.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all pending reminders after device reboot.
 * AlarmManager alarms are lost on reboot, so we need to restore them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val db = AppDatabase.getInstance(context)
        val scheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pendingReminders = db.reminderDao().getAllPendingReminders()
                val now = System.currentTimeMillis()

                for (reminder in pendingReminders) {
                    if (reminder.fireAt > now) {
                        // Still in the future — reschedule
                        scheduler.scheduleAlarm(
                            reminderId = reminder.id,
                            itemId = reminder.itemId,
                            notificationId = reminder.notificationId,
                            fireAtMs = reminder.fireAt,
                        )
                    } else {
                        // Already past due — fire immediately via AlarmReceiver logic
                        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
                            action = AlarmReceiver.ACTION_REMINDER_FIRED
                            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
                            putExtra(AlarmReceiver.EXTRA_ITEM_ID, reminder.itemId)
                            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, reminder.notificationId)
                        }
                        context.sendBroadcast(fireIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
