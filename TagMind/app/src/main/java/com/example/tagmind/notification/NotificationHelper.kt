package com.example.tagmind.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tagmind.MainActivity
import com.example.tagmind.R
import com.example.tagmind.alarm.AlarmReceiver

/**
 * Creates notification channels and posts reminder notifications.
 *
 * The reminder is posted as an alarm-category notification with a full-screen
 * intent, so it pops up on screen (even on the lock screen) instead of only
 * landing quietly in the shade.
 */
class NotificationHelper(private val context: Context) {

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        // The first channel was created without sound settings, and Android never lets an
        // app change a channel after creation, so it was replaced.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_REMINDERS)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Item reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when it's time to grab your items"
            enableVibration(true)
            vibrationPattern = VIBRATION
            enableLights(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    /** True when the system will actually display our notifications. */
    fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminderNotification(
        notificationId: Int,
        itemName: String,
        itemEmoji: String,
        reminderId: String,
        itemId: String,
    ) {
        if (!canNotify()) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ALARM
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_ITEM_ID, itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            notificationId * 31 + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val confirmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CONFIRM
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val confirmPending = PendingIntent.getBroadcast(
            context,
            notificationId * 31 + 2,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_tagmind)
            .setContentTitle("$itemEmoji Time to grab your $itemName")
            .setContentText("Tap your phone on the NFC tag to confirm you have it.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "I have it", confirmPending)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    companion object {
        const val CHANNEL_REMINDERS = "tagmind_reminders_v2"
        private const val LEGACY_CHANNEL_REMINDERS = "tagmind_reminders"
        private val VIBRATION = longArrayOf(0, 500, 200, 500)
        const val ACTION_OPEN_ALARM = "com.example.tagmind.ACTION_OPEN_ALARM"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_ITEM_ID = "extra_item_id"
    }
}
