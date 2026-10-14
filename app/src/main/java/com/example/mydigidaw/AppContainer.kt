package com.example.mydigidaw

import android.content.Context
import com.example.mydigidaw.data.backup.BackupManager
import com.example.mydigidaw.data.db.AppDatabase
import com.example.mydigidaw.data.google.GoogleCalendarSource
import com.example.mydigidaw.data.prefs.SettingsRepository
import com.example.mydigidaw.data.repo.CalendarRepository
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.data.repo.FocusRepository
import com.example.mydigidaw.data.repo.HabitsRepository
import com.example.mydigidaw.data.repo.JournalRepository
import com.example.mydigidaw.data.repo.NotesRepository
import com.example.mydigidaw.data.repo.TasksRepository
import com.example.mydigidaw.data.repo.VaultRepository
import com.example.mydigidaw.reminders.ReminderScheduler
import com.example.mydigidaw.security.AppLockManager
import com.example.mydigidaw.security.VaultManager
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Dependency injection manual: satu instans untuk seluruh aplikasi. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("Mydigidaw", "Uncaught exception in appScope", throwable)
    }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)

    val db: AppDatabase = AppDatabase.build(appContext)
    val settings = SettingsRepository(appContext)
    val vault = VaultManager(settings)
    val google = GoogleCalendarSource(appContext)
    val reminders = ReminderScheduler(appContext, db)

    val notes = NotesRepository(db.notes())
    val vaultItems = VaultRepository(db.vault(), vault)
    val finance = FinanceRepository(db, reminders)
    val calendar = CalendarRepository(db, google, reminders)
    val tasks = TasksRepository(db.tasks(), reminders)
    val habits = HabitsRepository(db.habits(), reminders)
    val focus = FocusRepository(appContext, db, settings, reminders)
    val journal = JournalRepository(db)
    val backup = BackupManager(appContext, db, settings, vault, reminders)

    val appLock = AppLockManager(settings, vault, appScope)
}
