package com.example.mydigidaw.data.repo

import android.content.Context
import com.example.mydigidaw.data.db.AppDatabase
import com.example.mydigidaw.data.db.FocusSessionEntity
import com.example.mydigidaw.data.db.JournalEntity
import com.example.mydigidaw.data.prefs.FocusState
import com.example.mydigidaw.data.prefs.SettingsRepository
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Timer fokus (Pomodoro). Status disimpan di DataStore sehingga timer tetap berjalan walau aplikasi
 * ditutup; akhir sesi ditandai alarm tepat waktu yang memunculkan notifikasi.
 */
class FocusRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
) {
    val state: Flow<FocusState> = settings.focusState
    val sessions: Flow<List<FocusSessionEntity>> = db.focus().observeAll()
    private val mutex = Mutex()

    suspend fun start(minutes: Int, label: String, isBreak: Boolean) = mutex.withLock {
        val current = settings.focusNow()
        if (current.active) finishLocked(current, completed = false)
        val now = System.currentTimeMillis()
        val id = if (isBreak) -1L else db.focus().insert(
            FocusSessionEntity(label = label.trim().ifBlank { "Fokus" }, startedAt = now, plannedMinutes = minutes),
        )
        val endAt = now + minutes * 60_000L
        settings.saveFocusState(FocusState(id, endAt, 0, minutes, label.trim(), isBreak))
        reminders.scheduleFocusEnd(endAt)
        Notifications.showFocusOngoing(context, endAt, label, isBreak)
    }

    suspend fun pause() = mutex.withLock {
        val s = settings.focusNow()
        if (!s.running) return@withLock
        val remaining = (s.endAt - System.currentTimeMillis()).coerceAtLeast(1_000)
        settings.saveFocusState(s.copy(endAt = 0, remainingMs = remaining))
        reminders.cancelFocus()
        Notifications.cancelFocusOngoing(context)
    }

    suspend fun resume() = mutex.withLock {
        val s = settings.focusNow()
        if (!s.paused) return@withLock
        val endAt = System.currentTimeMillis() + s.remainingMs
        settings.saveFocusState(s.copy(endAt = endAt, remainingMs = 0))
        reminders.scheduleFocusEnd(endAt)
        Notifications.showFocusOngoing(context, endAt, s.label, s.isBreak)
    }

    /** Menghentikan sesi lebih awal; menit yang sudah berjalan tetap dicatat. */
    suspend fun stop() = mutex.withLock {
        val s = settings.focusNow()
        if (s.active) finishLocked(s, completed = false)
    }

    /** Dipanggil saat waktu habis (dari alarm atau layar). Aman dipanggil berulang. Mengembalikan status yang selesai. */
    suspend fun complete(): FocusState? = mutex.withLock {
        val s = settings.focusNow()
        if (!s.running || s.endAt > System.currentTimeMillis() + 1_000) return@withLock null
        finishLocked(s, completed = true)
        s
    }

    private suspend fun finishLocked(s: FocusState, completed: Boolean) {
        if (s.sessionId > 0) {
            db.focus().get(s.sessionId)?.let { session ->
                val totalMs = s.plannedMinutes * 60_000L
                val remaining = if (s.running) (s.endAt - System.currentTimeMillis()).coerceAtLeast(0) else s.remainingMs
                val minutes = if (completed) s.plannedMinutes else ((totalMs - remaining) / 60_000L).toInt()
                if (!completed && minutes < 1) db.focus().delete(session.id)
                else db.focus().update(session.copy(actualMinutes = minutes, completed = completed))
            }
        }
        settings.saveFocusState(null)
        reminders.cancelFocus()
        Notifications.cancelFocusOngoing(context)
    }

    suspend fun deleteSession(id: Long) = db.focus().delete(id)

    /** Pastikan alarm & notifikasi berjalan lagi setelah HP dinyalakan ulang. */
    suspend fun restore() {
        val s = settings.focusNow()
        if (!s.running) return
        if (s.endAt <= System.currentTimeMillis()) {
            complete()?.let { Notifications.showFocusDone(context, it.label, it.plannedMinutes, it.isBreak) }
        } else {
            reminders.scheduleFocusEnd(s.endAt)
            Notifications.showFocusOngoing(context, s.endAt, s.label, s.isBreak)
        }
    }
}

class JournalRepository(private val db: AppDatabase) {
    val entries: Flow<List<JournalEntity>> = db.journal().observeAll()

    fun observeDay(day: LocalDate): Flow<JournalEntity?> = db.journal().observeDay(day.toEpochDay())

    suspend fun save(day: LocalDate, mood: Int, note: String) =
        db.journal().upsert(JournalEntity(day.toEpochDay(), mood.coerceIn(1, 5), note.trim(), System.currentTimeMillis()))

    suspend fun delete(day: LocalDate) = db.journal().delete(day.toEpochDay())
}

object Moods {
    val emoji = listOf("😞", "😕", "😐", "🙂", "😄")
    val label = listOf("Buruk", "Kurang", "Biasa", "Baik", "Luar biasa")

    fun emojiOf(mood: Int): String = emoji[(mood - 1).coerceIn(0, 4)]
    fun labelOf(mood: Int): String = label[(mood - 1).coerceIn(0, 4)]
}
