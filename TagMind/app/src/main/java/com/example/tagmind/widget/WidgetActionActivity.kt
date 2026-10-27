package com.example.tagmind.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.tagmind.MainActivity
import com.example.tagmind.notification.NotificationHelper

/**
 * Invisible stop for widget row taps. Opening an item needs an activity launch,
 * which Android only allows from a widget tap, so every row goes through here:
 * "open" forwards to the app, anything else is handed to [WidgetActionReceiver].
 */
class WidgetActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val op = intent.getStringExtra(WidgetActions.EXTRA_OP)
        val itemId = intent.getStringExtra(WidgetActions.EXTRA_ITEM_ID)

        if (op != null && itemId != null) {
            if (op == WidgetActions.OP_OPEN) {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        action = NotificationHelper.ACTION_OPEN_ALARM
                        putExtra(NotificationHelper.EXTRA_ITEM_ID, itemId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
            } else {
                sendBroadcast(
                    Intent(this, WidgetActionReceiver::class.java)
                        .putExtra(WidgetActions.EXTRA_OP, op)
                        .putExtra(WidgetActions.EXTRA_ITEM_ID, itemId)
                )
            }
        }
        finish()
    }
}
