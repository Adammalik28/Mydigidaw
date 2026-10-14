package com.example.mydigidaw.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupScheduleTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun neverBackedUp_isDueImmediately() {
        assertTrue(MaintenanceWorker.isDue(lastAt = 0, everyDays = 7, now = 1_000 * day))
    }

    @Test
    fun weekly_runsAboutOnceAWeek() {
        val last = 1_000 * day
        assertFalse(MaintenanceWorker.isDue(last, 7, last + 6 * day))
        assertTrue(MaintenanceWorker.isDue(last, 7, last + 6 * day + 18 * hour))
    }

    @Test
    fun daily_toleratesTheTwelveHourWorkerCadence() {
        val last = 1_000 * day
        assertFalse(MaintenanceWorker.isDue(last, 1, last + 12 * hour))
        assertTrue(MaintenanceWorker.isDue(last, 1, last + 18 * hour))
        assertTrue(MaintenanceWorker.isDue(last, 1, last + 24 * hour))
    }
}
