package com.example.mydigidaw.data.model

import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDateTime
import com.example.mydigidaw.util.toMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RecurrenceTest {
    @Test
    fun monthly_clampsToLastDayOfShortMonths() {
        val jan31 = LocalDateTime.of(2026, 1, 31, 9, 0)
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0), Recurrence.nth(jan31, Repeat.MONTHLY, 1))
        assertEquals(LocalDateTime.of(2026, 3, 31, 9, 0), Recurrence.nth(jan31, Repeat.MONTHLY, 2))
    }

    @Test
    fun weeklyEvent_occursEveryMondayInOctober() {
        val start = LocalDateTime.of(2026, 10, 5, 9, 0).toMillis() // Senin
        val end = LocalDateTime.of(2026, 10, 5, 10, 0).toMillis()
        val from = LocalDate.of(2026, 10, 1).startMillis()
        val to = LocalDate.of(2026, 11, 1).startMillis()
        val days = Recurrence.occurrences(start, end, Repeat.WEEKLY, from, to).map { it.first.toLocalDateTime().dayOfMonth }
        assertEquals(listOf(5, 12, 19, 26), days)
    }

    @Test
    fun occurrences_keepDurationAndIncludeOverlappingEvents() {
        // Acara 23:00–01:00 yang dimulai sehari sebelum rentang tetap terlihat karena beririsan.
        val start = LocalDateTime.of(2026, 10, 9, 23, 0).toMillis()
        val end = LocalDateTime.of(2026, 10, 10, 1, 0).toMillis()
        val result = Recurrence.occurrences(start, end, Repeat.NONE, LocalDate.of(2026, 10, 10).startMillis(), LocalDate.of(2026, 10, 11).startMillis())
        assertEquals(listOf(start to end), result)
    }

    @Test
    fun oneOffEventOutsideRange_isIgnored() {
        val start = LocalDateTime.of(2026, 9, 1, 9, 0).toMillis()
        assertTrue(Recurrence.occurrences(start, start + 3_600_000, Repeat.NONE, LocalDate.of(2026, 10, 1).startMillis(), LocalDate.of(2026, 11, 1).startMillis()).isEmpty())
    }

    @Test
    fun nextStartAtOrAfter_findsTheNextDailyOccurrence() {
        val start = LocalDateTime.of(2026, 10, 1, 7, 0).toMillis()
        val now = LocalDateTime.of(2026, 10, 15, 8, 0).toMillis()
        assertEquals(LocalDateTime.of(2026, 10, 16, 7, 0).toMillis(), Recurrence.nextStartAtOrAfter(start, Repeat.DAILY, now))
        assertNull(Recurrence.nextStartAtOrAfter(start, Repeat.NONE, now))
        assertEquals(start, Recurrence.nextStartAtOrAfter(start, Repeat.NONE, start - 1))
    }
}

class HabitLogicTest {
    private val created = LocalDate.of(2026, 10, 1).atTime(8, 0).toMillis() // Kamis
    private val daily = HabitEntity(1, "Baca buku", "📖", 0, createdAt = created)
    private val weekdays = HabitEntity(2, "Gym", "🏋️", 1, daysMask = 0b0011111, createdAt = created)

    private fun logs(vararg days: Int, count: Int = 1) = days.associate { LocalDate.of(2026, 10, it).toEpochDay() to count }

    @Test
    fun unfinishedToday_doesNotBreakTheStreak() {
        val counts = logs(10, 11, 12, 13, 14)
        assertEquals(5, HabitLogic.currentStreak(daily, counts, LocalDate.of(2026, 10, 15)))
        assertEquals(6, HabitLogic.currentStreak(daily, counts + logs(15), LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun missedDay_resetsTheStreak() {
        assertEquals(1, HabitLogic.currentStreak(daily, logs(10, 11, 13), LocalDate.of(2026, 10, 13)))
        assertEquals(2, HabitLogic.bestStreak(daily, logs(10, 11, 13), LocalDate.of(2026, 10, 13)))
    }

    @Test
    fun unscheduledWeekendDays_areSkipped() {
        // Jumat 9 & Senin 12 selesai; Sabtu–Minggu tidak dijadwalkan, Kamis 8 terlewat.
        assertEquals(2, HabitLogic.currentStreak(weekdays, logs(9, 12), LocalDate.of(2026, 10, 13)))
        assertTrue(HabitLogic.isScheduled(weekdays, LocalDate.of(2026, 10, 12)))
        assertTrue(!HabitLogic.isScheduled(weekdays, LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun multiCountHabits_needTheFullTarget() {
        val water = daily.copy(targetPerDay = 2)
        assertEquals(0, HabitLogic.currentStreak(water, logs(14, count = 1), LocalDate.of(2026, 10, 15)))
        assertEquals(1, HabitLogic.currentStreak(water, logs(14, count = 2), LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun completionRate_ignoresTodayUntilDone() {
        // 1–14 Okt: 14 hari terjadwal, 7 selesai; hari ini (15) belum dihitung.
        val counts = logs(1, 2, 3, 4, 5, 6, 7)
        assertEquals(0.5f, HabitLogic.completionRate(daily, counts, LocalDate.of(2026, 10, 15)), 0.0001f)
    }
}
