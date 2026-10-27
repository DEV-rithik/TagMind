package com.example.tagmind.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tagmind.TagMindApplication
import com.example.tagmind.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Runs a reminder action (start, cancel, confirm) requested from the widget. */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(WidgetActions.EXTRA_ITEM_ID) ?: return
        val op = intent.getStringExtra(WidgetActions.EXTRA_OP) ?: return

        val app = context.applicationContext as TagMindApplication
        val repo = app.repository
        val scheduler = app.alarmScheduler
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val active = repo.getActiveReminderForItem(itemId)
                when (op) {
                    WidgetActions.OP_START -> {
                        val fireAt = System.currentTimeMillis() +
                            TimeUnit.MINUTES.toMillis(WidgetActions.QUICK_REMINDER_MINUTES)
                        val reminder = repo.createReminder(itemId = itemId, fireAt = fireAt)
                        scheduler.scheduleAlarm(
                            reminderId = reminder.id,
                            itemId = itemId,
                            notificationId = reminder.notificationId,
                            fireAtMs = fireAt,
                        )
                    }
                    WidgetActions.OP_CANCEL -> active?.let {
                        scheduler.cancelAlarm(it.notificationId)
                        repo.cancelReminder(it.id)
                    }
                    WidgetActions.OP_CONFIRM -> active?.let {
                        scheduler.cancelAlarm(it.notificationId)
                        repo.confirmReminder(it.id)
                        NotificationHelper(context).cancelNotification(it.notificationId)
                    }
                }
            } finally {
                TagMindWidgetProvider.refreshNow(context)
                pending.finish()
            }
        }
    }
}
