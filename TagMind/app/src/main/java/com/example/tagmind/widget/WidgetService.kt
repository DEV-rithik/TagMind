package com.example.tagmind.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.tagmind.R
import com.example.tagmind.data.db.entity.ReminderStatus
import kotlinx.coroutines.runBlocking
import java.text.DateFormat
import java.util.Date

/** Supplies the rows of the widget list. */
class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {

        private var rows: List<WidgetRow> = emptyList()

        override fun onCreate() = Unit

        // Called on a binder thread, so blocking here is allowed
        override fun onDataSetChanged() {
            rows = runBlocking { TagMindWidgetProvider.loadRows(context) }
        }

        override fun onDestroy() {
            rows = emptyList()
        }

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews? {
            val row = rows.getOrNull(position) ?: return null
            val views = RemoteViews(context.packageName, R.layout.widget_row)

            views.setTextViewText(R.id.row_emoji, row.emoji)
            views.setTextViewText(R.id.row_name, row.name)

            val (statusText, statusColor, actionLabel, actionOp) = when (row.state) {
                ReminderStatus.FIRED -> RowStyle(
                    "Grab it now", COLOR_DUE, "Got it", WidgetActions.OP_CONFIRM,
                )
                ReminderStatus.PENDING -> RowStyle(
                    "Reminder at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(row.fireAt))}",
                    COLOR_TEXT, "Cancel", WidgetActions.OP_CANCEL,
                )
                else -> RowStyle(
                    "No reminder", COLOR_MUTED, "+${WidgetActions.QUICK_REMINDER_MINUTES}m", WidgetActions.OP_START,
                )
            }
            views.setTextViewText(R.id.row_status, statusText)
            views.setTextColor(R.id.row_status, statusColor)
            views.setTextViewText(R.id.row_action, actionLabel)

            views.setOnClickFillInIntent(
                R.id.row_info,
                Intent()
                    .putExtra(WidgetActions.EXTRA_OP, WidgetActions.OP_OPEN)
                    .putExtra(WidgetActions.EXTRA_ITEM_ID, row.itemId),
            )
            views.setOnClickFillInIntent(
                R.id.row_action,
                Intent()
                    .putExtra(WidgetActions.EXTRA_OP, actionOp)
                    .putExtra(WidgetActions.EXTRA_ITEM_ID, row.itemId),
            )
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = rows.getOrNull(position)?.itemId.hashCode().toLong()
        override fun hasStableIds(): Boolean = true
    }

    private data class RowStyle(val status: String, val color: Int, val actionLabel: String, val op: String)

    private companion object {
        val COLOR_DUE = Color.parseColor("#D71921")
        val COLOR_TEXT = Color.parseColor("#FFFFFF")
        val COLOR_MUTED = Color.parseColor("#8A8A8A")
    }
}
