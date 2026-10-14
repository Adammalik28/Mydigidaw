package com.example.mydigidaw.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mydigidaw.MydigidawApp
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Pemeliharaan berkala (± setiap 12 jam, walau aplikasi tidak dibuka): mencatat transaksi berulang
 * yang jatuh tempo, menjadwalkan ulang pengingat, membuat cadangan otomatis (harian/mingguan
 * sesuai pengaturan), dan menyegarkan widget.
 */
class MaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as MydigidawApp).container
        runCatching { c.finance.processRecurring() }
        runCatching { c.reminders.rescheduleAll() }
        val s = c.settings.current()
        if (s.autoBackupUri != null && isDue(s.autoBackupLastAt, s.autoBackupEveryDays, System.currentTimeMillis())) {
            runCatching { c.backup.runAutoBackup() }
                .onSuccess { Notifications.cancel(applicationContext, Notifications.BACKUP_FAILED_ID) }
                .onFailure { e ->
                    // Beri tahu sekali saja; pesan berikutnya muncul lagi setelah ada cadangan yang berhasil.
                    if (s.autoBackupError == null) Notifications.showBackupFailed(applicationContext, e.message ?: "galat tidak dikenal")
                }
        }
        runCatching { WidgetUpdater.updateNow(applicationContext) }
        return Result.success()
    }

    companion object {
        private const val NAME = "mydigidaw-maintenance"
        private const val HOUR_MS = 3_600_000L

        /**
         * Pekerja berjalan ±12 jam sekali, jadi beri kelonggaran 6 jam: cadangan harian tidak
         * terlewat sehari penuh dan cadangan mingguan tetap ±7 hari sekali.
         */
        fun isDue(lastAt: Long, everyDays: Int, now: Long): Boolean =
            now - lastAt >= everyDays.coerceAtLeast(1) * 24 * HOUR_MS - 6 * HOUR_MS

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
