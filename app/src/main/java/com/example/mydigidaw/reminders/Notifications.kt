package com.example.mydigidaw.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mydigidaw.MainActivity
import com.example.mydigidaw.R
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.toLocalDate

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_HABITS = "habits"
    const val CHANNEL_FOCUS = "focus"
    const val CHANNEL_FINANCE = "finance"
    const val CHANNEL_SYSTEM = "system"
    const val BACKUP_FAILED_ID = 4_000_002
    const val FOCUS_ONGOING_ID = 4_000_001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Pengingat jadwal & tugas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi sebelum jadwal dimulai dan saat tugas jatuh tempo"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_HABITS, "Pengingat kebiasaan", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pengingat harian untuk kebiasaan yang Anda lacak"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_FOCUS, "Timer fokus berjalan", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Hitung mundur sesi fokus yang sedang berjalan"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_FINANCE, "Keuangan", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Peringatan anggaran, tagihan rutin, dan jatuh tempo utang"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SYSTEM, "Cadangan data", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pemberitahuan bila cadangan otomatis gagal"
            },
        )
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notificationId(kind: Int, id: Long): Int = kind * 10_000_000 + (id % 10_000_000).toInt()

    private fun contentIntent(context: Context, deepLink: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLink.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(context: Context, action: String, kind: Int, id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            data = "mydigidaw://action/$action/$kind/$id".toUri()
            putExtra(ReminderReceiver.EXTRA_KIND, kind)
            putExtra(ReminderReceiver.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(context, notificationId(kind, id), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showTask(context: Context, task: TaskEntity) {
        val id = notificationId(ReminderScheduler.KIND_TASK, task.id)
        val due = task.dueAt?.let { Dates.time(it) }
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Tugas: ${task.title}")
            .setContentText(if (due != null) "Jatuh tempo pukul $due" else "Saatnya mengerjakan tugas ini")
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.notes.ifBlank { "Jatuh tempo pukul ${due ?: "-"}" }))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "mydigidaw://open/tasks", id))
            .addAction(0, "Tandai selesai", actionIntent(context, ReminderReceiver.ACTION_TASK_DONE, ReminderScheduler.KIND_TASK, task.id))
        post(context, id, builder)
    }

    fun showEvent(context: Context, event: EventEntity, occurrenceStart: Long) {
        val id = notificationId(ReminderScheduler.KIND_EVENT, event.id)
        val duration = event.endAt - event.startAt
        val whenText = if (event.allDay) {
            "Sepanjang hari · ${Dates.relativeDay(occurrenceStart.toLocalDate())}"
        } else {
            "${Dates.relativeDay(occurrenceStart.toLocalDate())}, ${Dates.time(occurrenceStart)} – ${Dates.time(occurrenceStart + duration)}"
        }
        val text = listOf(whenText, event.location).filter { it.isNotBlank() }.joinToString(" · ")
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(event.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOf(text, event.description).filter { it.isNotBlank() }.joinToString("\n")))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "mydigidaw://open/calendar", id))
        post(context, id, builder)
    }

    fun showHabit(context: Context, habit: HabitEntity, streak: Int) {
        val id = notificationId(ReminderScheduler.KIND_HABIT, habit.id)
        val text = if (streak > 0) "Streak Anda $streak hari — jangan sampai putus!" else "Yuk mulai hari ini. Sedikit demi sedikit jadi bukit."
        val builder = NotificationCompat.Builder(context, CHANNEL_HABITS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Waktunya ${habit.emoji} ${habit.name}")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "mydigidaw://open/habits", id))
            .addAction(0, "Sudah", actionIntent(context, ReminderReceiver.ACTION_HABIT_DONE, ReminderScheduler.KIND_HABIT, habit.id))
        post(context, id, builder)
    }

    fun showFocusOngoing(context: Context, endAt: Long, label: String, isBreak: Boolean) {
        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(if (isBreak) "☕ Istirahat sejenak" else "🎯 ${label.ifBlank { "Sesi fokus" }}")
            .setContentText("Selesai pukul ${Dates.time(endAt)}")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent(context, "mydigidaw://open/focus", FOCUS_ONGOING_ID))
        post(context, FOCUS_ONGOING_ID, builder)
    }

    fun cancelFocusOngoing(context: Context) = cancel(context, FOCUS_ONGOING_ID)

    fun showFocusDone(context: Context, label: String, minutes: Int, wasBreak: Boolean) {
        val id = notificationId(ReminderScheduler.KIND_FOCUS, 1)
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(if (wasBreak) "Istirahat selesai" else "Sesi fokus selesai 🎉")
            .setContentText(
                if (wasBreak) "Siap kembali fokus?" else "$minutes menit ${label.ifBlank { "fokus" }} tuntas. Waktunya istirahat sebentar.",
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "mydigidaw://open/focus", id))
        post(context, id, builder)
    }

    fun showFinance(context: Context, key: Int, title: String, text: String, deepLink: String = "mydigidaw://open/finance") {
        val builder = NotificationCompat.Builder(context, CHANNEL_FINANCE)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, deepLink, key))
        post(context, key, builder)
    }

    fun showBackupFailed(context: Context, reason: String) {
        val text = "Data Anda tidak tercadangkan: $reason. Ketuk untuk memeriksa pengaturan cadangan."
        val builder = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Cadangan otomatis gagal")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "mydigidaw://open/settings", BACKUP_FAILED_ID))
        post(context, BACKUP_FAILED_ID, builder)
    }

    fun showTest(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Notifikasi berfungsi 🎉")
            .setContentText("Pengingat jadwal, tugas, dan kebiasaan akan muncul seperti ini.")
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "mydigidaw://open/home", 1))
        post(context, 1, builder)
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
