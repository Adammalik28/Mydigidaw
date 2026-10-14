package com.example.mydigidaw

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.mydigidaw.data.db.DefaultData
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.widget.WidgetUpdater
import com.example.mydigidaw.work.MaintenanceWorker
import kotlinx.coroutines.launch

class MydigidawApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        runCatching { Notifications.createChannels(this) }

        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(container.appLock)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Catat transaksi berulang yang jatuh tempo setiap kali aplikasi dibuka.
                container.appScope.launch { runCatching { container.finance.processRecurring() } }
            }
        })

        container.appScope.launch {
            runCatching { DefaultData.seedIfEmpty(container.db) }
            runCatching { container.reminders.rescheduleAll() }
            runCatching { container.focus.restore() }
        }
        runCatching { MaintenanceWorker.schedule(this) }
        runCatching { WidgetUpdater.start(this, container) }
    }
}
