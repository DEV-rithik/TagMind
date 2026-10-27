package com.example.tagmind.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import com.example.tagmind.MainActivity
import com.example.tagmind.R
import com.example.tagmind.data.db.AppDatabase
import com.example.tagmind.data.db.entity.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Intent extras shared by the widget, its click trampoline and its action receiver. */
object WidgetActions {
    const val EXTRA_OP = "widget_op"
    const val EXTRA_ITEM_ID = "widget_item_id"

    const val OP_OPEN = "open"
    const val OP_START = "start"
    const val OP_CANCEL = "cancel"
    const val OP_CONFIRM = "confirm"

    /** Delay used by the widget's one-tap reminder button. */
    const val QUICK_REMINDER_MINUTES = 30L
}

/** One line of the widget list. */
data class WidgetRow(
    val itemId: String,
    val emoji: String,
    val name: String,
    val state: String,       // ReminderStatus.PENDING / FIRED, or "" when nothing is active
    val fireAt: Long,
)

/**
 * Home-screen widget listing every item with its reminder state.
 * Each row opens the item, and its button starts, cancels or confirms a reminder
 * without opening the app.
 */
class TagMindWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                refreshNow(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        /** Fire-and-forget refresh for callers that are not already in a coroutine. */
        fun refresh(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch { refreshNow(appContext) }
        }

        /** Rebuilds every placed widget and asks its list to reload. */
        suspend fun refreshNow(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TagMindWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val summary = withContext(Dispatchers.IO) { summaryText(loadRows(context)) }
            ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, id, summary)) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        /** Rows ordered by urgency: due now, then upcoming, then idle items. */
        suspend fun loadRows(context: Context): List<WidgetRow> {
            val db = AppDatabase.getInstance(context)
            val items = db.itemDao().getAllItems().first()
            val active = db.reminderDao().getActiveReminders().first().associateBy { it.itemId }
            return items
                .map { item ->
                    val reminder = active[item.id]
                    WidgetRow(
                        itemId = item.id,
                        emoji = item.iconEmoji,
                        name = item.name,
                        state = reminder?.status ?: "",
                        fireAt = reminder?.fireAt ?: 0L,
                    )
                }
                .sortedWith(
                    compareBy<WidgetRow> {
                        when (it.state) {
                            ReminderStatus.FIRED -> 0
                            ReminderStatus.PENDING -> 1
                            else -> 2
                        }
                    }.thenBy { if (it.state == ReminderStatus.PENDING) it.fireAt else Long.MAX_VALUE }
                )
        }

        private fun summaryText(rows: List<WidgetRow>): String {
            val due = rows.count { it.state == ReminderStatus.FIRED }
            val upcoming = rows.count { it.state == ReminderStatus.PENDING }
            return when {
                due > 0 -> "$due to grab now"
                upcoming > 0 -> if (upcoming == 1) "1 reminder set" else "$upcoming reminders set"
                rows.isEmpty() -> ""
                else -> "No reminders"
            }
        }

        private fun buildViews(context: Context, widgetId: Int, summary: String): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_tagmind)

            views.setTextViewText(R.id.widget_summary, summary)

            // Tapping the header opens the app
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_header, openApp)

            // The list is served by WidgetService; each row fills in this template
            val serviceIntent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val template = PendingIntent.getActivity(
                context,
                1,
                Intent(context, WidgetActionActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            views.setPendingIntentTemplate(R.id.widget_list, template)
            return views
        }
    }
}
