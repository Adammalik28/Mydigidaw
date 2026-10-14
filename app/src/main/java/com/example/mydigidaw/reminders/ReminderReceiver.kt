package com.example.mydigidaw.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.MydigidawApp
import com.example.mydigidaw.data.model.DebtType
import com.example.mydigidaw.data.model.HabitLogic
import com.example.mydigidaw.util.Money
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MydigidawApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                handle(container, context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(c: AppContainer, context: Context, intent: Intent) {
        val kind = intent.getIntExtra(EXTRA_KIND, 0)
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_REMIND -> when (kind) {
                ReminderScheduler.KIND_TASK -> {
                    val task = c.db.tasks().get(id) ?: return
                    if (!task.isDone && task.remind && task.hasTime) Notifications.showTask(context, task)
                }
                ReminderScheduler.KIND_EVENT -> {
                    val event = c.db.events().get(id) ?: return
                    if (event.reminderMinutes >= 0) {
                        Notifications.showEvent(context, event, intent.getLongExtra(EXTRA_OCCURRENCE, event.startAt))
                    }
                    c.reminders.scheduleEvent(event)
                }
                ReminderScheduler.KIND_FOCUS -> {
                    c.focus.complete()?.let { done ->
                        Notifications.showFocusDone(context, done.label, done.plannedMinutes, done.isBreak)
                    }
                }
                ReminderScheduler.KIND_BILL -> {
                    val rule = c.db.recurring().all().firstOrNull { it.id == id } ?: return
                    if (!rule.isActive) return
                    val category = rule.categoryId?.let { cid -> c.db.categories().all().firstOrNull { it.id == cid } }
                    val name = category?.let { "${it.emoji} ${it.name}" } ?: rule.note.ifBlank { "Tagihan rutin" }
                    Notifications.showFinance(
                        context,
                        Notifications.notificationId(kind, id),
                        "Besok: $name",
                        "${Money.format(rule.amount)} akan dicatat otomatis besok" + if (rule.note.isNotBlank()) " · ${rule.note}" else "",
                    )
                }
                ReminderScheduler.KIND_DEBT -> {
                    val debt = c.db.debts().all().firstOrNull { it.id == id } ?: return
                    if (debt.isSettled) return
                    val remaining = Money.format(debt.amount - debt.paidAmount)
                    val payable = debt.type == DebtType.PAYABLE.name
                    Notifications.showFinance(
                        context,
                        Notifications.notificationId(kind, id),
                        if (payable) "Jatuh tempo: utang ke ${debt.person}" else "Jatuh tempo: piutang dari ${debt.person}",
                        if (payable) "Sisa $remaining perlu dibayar hari ini." else "Sisa $remaining seharusnya diterima hari ini.",
                        "mydigidaw://open/debts",
                    )
                }
                ReminderScheduler.KIND_HABIT -> {
                    val habit = c.db.habits().get(id) ?: return
                    val today = LocalDate.now()
                    val active = habit.reminderMinutes != null && !habit.isArchived && HabitLogic.isScheduled(habit, today)
                    if (active && c.habits.count(habit.id, today) < habit.targetPerDay) {
                        val logs = c.habits.logsFor(habit.id).first().associate { it.day to it.count }
                        Notifications.showHabit(context, habit, HabitLogic.currentStreak(habit, logs, today))
                    }
                    c.reminders.scheduleHabit(habit)
                }
            }
            ACTION_TASK_DONE -> {
                c.tasks.setDone(id, true)
                Notifications.cancel(context, Notifications.notificationId(kind, id))
            }
            ACTION_HABIT_DONE -> {
                c.habits.increment(id, LocalDate.now())
                Notifications.cancel(context, Notifications.notificationId(kind, id))
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.example.mydigidaw.action.REMIND"
        const val ACTION_TASK_DONE = "com.example.mydigidaw.action.TASK_DONE"
        const val ACTION_HABIT_DONE = "com.example.mydigidaw.action.HABIT_DONE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val EXTRA_OCCURRENCE = "occurrence"
    }
}

/** Alarm hilang saat HP dimulai ulang atau jam diubah, jadi jadwalkan ulang semuanya. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val container = (context.applicationContext as MydigidawApp).container
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.reminders.rescheduleAll()
                container.focus.restore()
            } finally {
                pending.finish()
            }
        }
    }
}

private val RESCHEDULE_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
)
