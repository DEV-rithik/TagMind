package com.example.tagmind

import android.app.Application
import com.example.tagmind.data.db.AppDatabase
import com.example.tagmind.data.repository.TagMindRepository
import com.example.tagmind.alarm.AlarmScheduler
import com.example.tagmind.widget.TagMindWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Application class that initializes app-wide singletons.
 * Manual DI — no Hilt to keep things simple for now.
 */
class TagMindApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: TagMindRepository
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = TagMindRepository(
            itemDao = database.itemDao(),
            reminderDao = database.reminderDao(),
            scanEventDao = database.scanEventDao(),
        )
        alarmScheduler = AlarmScheduler(this)

        // Redraw the home-screen widget whenever items or reminders change
        appScope.launch {
            combine(repository.getAllItems(), repository.getActiveReminders()) { _, _ -> }
                .debounce(300)
                .collect { TagMindWidgetProvider.refreshNow(this@TagMindApplication) }
        }
    }
}
