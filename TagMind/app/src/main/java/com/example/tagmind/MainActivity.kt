package com.example.tagmind

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.SystemClock
import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tagmind.nfc.NfcIntentParser
import com.example.tagmind.notification.NotificationHelper
import com.example.tagmind.theme.DarkSurface
import com.example.tagmind.theme.TagMindTheme

/** Something the user has to switch on in system settings for reminders to reach them. */
private enum class PermissionIssue(val title: String, val message: String) {
    NOTIFICATIONS(
        "Turn on notifications",
        "TagMind needs notifications to tell you when it's time to grab an item.",
    ),
    EXACT_ALARM(
        "Allow alarms and reminders",
        "Without this, reminders can arrive several minutes late.",
    ),
    FULL_SCREEN(
        "Allow full-screen alerts",
        "This lets a reminder pop up on your screen, even when the phone is locked.",
    ),
}

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // Shared NFC scan callback — set by the active screen via LocalNfcScanHandler
    var onNfcTagScanned: ((String) -> Unit)? by mutableStateOf(null)
    private var pendingUid: String? by mutableStateOf(null)
    private var alarmItemId: String? by mutableStateOf(null)
    private var lastUid: String? = null
    private var lastScanAt = 0L
    private var permissionIssue: PermissionIssue? by mutableStateOf(null)
    private val dismissedIssues = mutableSetOf<PermissionIssue>()

    private var permissionPromptAnswered = true

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionPromptAnswered = true
            refreshPermissionIssue()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Handle alarm notification tap that opens the alarm screen
        alarmItemId = intent.getStringExtra(NotificationHelper.EXTRA_ITEM_ID)
        handleTagIntent(intent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionPromptAnswered = false
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TagMindTheme {
                MainNavigation(
                    initialAlarmItemId = alarmItemId,
                    pendingNfcUid = pendingUid,
                    onAlarmOpened = { alarmItemId = null },
                )
                LaunchedEffect(pendingUid, onNfcTagScanned) {
                    val uid = pendingUid
                    val handler = onNfcTagScanned
                    if (uid != null && handler != null) {
                        pendingUid = null
                        handler(uid)
                    }
                }
                permissionIssue?.let { issue ->
                    AlertDialog(
                        onDismissRequest = {
                            dismissedIssues += issue
                            refreshPermissionIssue()
                        },
                        containerColor = DarkSurface,
                        title = { Text(issue.title) },
                        text = { Text(issue.message) },
                        confirmButton = {
                            TextButton(onClick = { openSettingsFor(issue) }) { Text("Open settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                dismissedIssues += issue
                                refreshPermissionIssue()
                            }) { Text("Not now") }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
        refreshPermissionIssue()
    }

    private fun refreshPermissionIssue() {
        val alarmManager = getSystemService(android.app.AlarmManager::class.java)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val granted = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        val notificationsOn = NotificationManagerCompat.from(this).areNotificationsEnabled()

        val issue = when {
            // Denied at the runtime prompt or switched off in settings
            !granted || !notificationsOn -> {
                // While the first-launch prompt is still unanswered, don't stack a dialog on it
                if (!granted && !permissionPromptAnswered) null else PermissionIssue.NOTIFICATIONS
            }
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms() -> PermissionIssue.EXACT_ALARM
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !notificationManager.canUseFullScreenIntent() -> PermissionIssue.FULL_SCREEN
            else -> null
        }
        permissionIssue = issue?.takeIf { it !in dismissedIssues }
    }

    private fun openSettingsFor(issue: PermissionIssue) {
        val intent = when (issue) {
            PermissionIssue.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            PermissionIssue.EXACT_ALARM -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            } else null
            PermissionIssue.FULL_SCREEN -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
            } else null
        }
        try {
            intent?.let(::startActivity)
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(NotificationHelper.EXTRA_ITEM_ID)?.let { alarmItemId = it }
        handleTagIntent(intent)
    }

    private fun handleTagIntent(intent: Intent) {
        val tag: Tag? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        val uid = NfcIntentParser.extractUid(tag)
        if (uid != null) {
            val now = SystemClock.elapsedRealtime()
            if (uid == lastUid && now - lastScanAt < 2_000L) return
            lastUid = uid
            lastScanAt = now
            pendingUid = uid
        }
    }

    private fun enableNfcForegroundDispatch() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        try {
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        } catch (_: Exception) { /* NFC not available */ }
    }
}
