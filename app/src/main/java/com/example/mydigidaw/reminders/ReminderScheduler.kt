package com.example.mydigidaw.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.example.mydigidaw.data.db.AppDatabase
import com.example.mydigidaw.data.db.DebtEntity
import com.example.mydigidaw.data.db.RecurringEntity
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.model.HabitLogic
import com.example.mydigidaw.data.model.Recurrence
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.util.toLocalDate
import com.example.mydigidaw.util.toMillis
import java.time.LocalDate
import java.time.LocalTime

/** Menjadwalkan alarm tepat waktu untuk pengingat tugas, jadwal, dan kebiasaan. */
class ReminderScheduler(private val context: Context, private val db: AppDatabase) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll() {
        db.tasks().all().forEach { scheduleTask(it) }
        db.events().all().forEach { scheduleEvent(it) }
        db.habits().allHabits().forEach { scheduleHabit(it) }
        db.recurring().all().forEach { scheduleBill(it) }
        db.debts().all().forEach { scheduleDebt(it) }
    }

    /** Membatalkan alarm semua data yang ada sekarang, mis. sebelum seluruh data diganti dari cadangan. */
    suspend fun cancelAll() {
        db.tasks().all().forEach { cancel(KIND_TASK, it.id) }
        db.events().all().forEach { cancel(KIND_EVENT, it.id) }
        db.habits().allHabits().forEach { cancel(KIND_HABIT, it.id) }
        db.recurring().all().forEach { cancel(KIND_BILL, it.id) }
        db.debts().all().forEach { cancel(KIND_DEBT, it.id) }
    }

    /** Pengingat tagihan rutin: pukul 19.00 sehari sebelum transaksi berulang dicatat. */
    fun scheduleBill(rule: RecurringEntity) {
        if (!rule.isActive || rule.type != TxType.EXPENSE.name) {
            cancel(KIND_BILL, rule.id)
            return
        }
        val at = rule.nextDate.toLocalDate().minusDays(1).atTime(19, 0).toMillis()
        if (at <= System.currentTimeMillis()) cancel(KIND_BILL, rule.id) else set(KIND_BILL, rule.id, at, rule.nextDate)
    }

    /** Pengingat utang/piutang: pukul 08.00 pada hari jatuh tempo. */
    fun scheduleDebt(debt: DebtEntity) {
        val due = debt.dueDate
        if (debt.isSettled || due == null) {
            cancel(KIND_DEBT, debt.id)
            return
        }
        val at = due.toLocalDate().atTime(8, 0).toMillis()
        if (at <= System.currentTimeMillis()) cancel(KIND_DEBT, debt.id) else set(KIND_DEBT, debt.id, at, due)
    }

    fun scheduleFocusEnd(endAt: Long) = set(KIND_FOCUS, 0, endAt, endAt)

    fun cancelFocus() = cancel(KIND_FOCUS, 0)

    fun scheduleTask(task: TaskEntity) {
        val due = task.dueAt
        if (task.isDone || !task.remind || !task.hasTime || due == null || due <= System.currentTimeMillis()) {
            cancel(KIND_TASK, task.id)
            return
        }
        set(KIND_TASK, task.id, due, due)
    }

    fun scheduleEvent(event: EventEntity) {
        if (event.reminderMinutes < 0) {
            cancel(KIND_EVENT, event.id)
            return
        }
        val next = nextEventTrigger(event, System.currentTimeMillis())
        if (next == null) cancel(KIND_EVENT, event.id) else set(KIND_EVENT, event.id, next.first, next.second)
    }

    fun scheduleHabit(habit: HabitEntity) {
        val minutes = habit.reminderMinutes
        if (minutes == null || habit.isArchived || habit.daysMask == 0) {
            cancel(KIND_HABIT, habit.id)
            return
        }
        val now = System.currentTimeMillis()
        val time = LocalTime.of(minutes / 60, minutes % 60)
        var day = LocalDate.now()
        repeat(8) {
            if (HabitLogic.isScheduled(habit, day)) {
                val at = day.atTime(time).toMillis()
                if (at > now) {
                    set(KIND_HABIT, habit.id, at, at)
                    return
                }
            }
            day = day.plusDays(1)
        }
        cancel(KIND_HABIT, habit.id)
    }

    fun cancel(kind: Int, id: Long) {
        alarmManager?.cancel(pendingIntent(kind, id, 0))
    }

    private fun set(kind: Int, id: Long, triggerAt: Long, occurrence: Long) {
        val manager = alarmManager ?: return
        val pi = pendingIntent(kind, id, occurrence)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || runCatching { manager.canScheduleExactAlarms() }.getOrDefault(false)
        try {
            if (exact) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: Exception) {
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi) }
        }
    }

    private fun pendingIntent(kind: Int, id: Long, occurrence: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            data = "mydigidaw://reminder/$kind/$id".toUri()
            putExtra(ReminderReceiver.EXTRA_KIND, kind)
            putExtra(ReminderReceiver.EXTRA_ID, id)
            putExtra(ReminderReceiver.EXTRA_OCCURRENCE, occurrence)
        }
        return PendingIntent.getBroadcast(
            context,
            Notifications.notificationId(kind, id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val KIND_TASK = 1
        const val KIND_EVENT = 2
        const val KIND_HABIT = 3
        const val KIND_FOCUS = 4
        const val KIND_BILL = 5
        const val KIND_DEBT = 6

        /** Acara sepanjang hari diingatkan relatif terhadap pukul 07.00 pada hari itu. */
        private const val ALL_DAY_ANCHOR_MS = 7 * 3_600_000L

        /** (waktu alarm, waktu mulai kejadian) berikutnya setelah [now]. */
        fun nextEventTrigger(event: EventEntity, now: Long): Pair<Long, Long>? {
            val offset = (if (event.allDay) ALL_DAY_ANCHOR_MS else 0L) - event.reminderMinutes * 60_000L
            val occurrence = Recurrence.nextStartAtOrAfter(event.startAt, enumOr(event.repeat, Repeat.NONE), now - offset + 1)
                ?: return null
            return (occurrence + offset) to occurrence
        }
    }
}
