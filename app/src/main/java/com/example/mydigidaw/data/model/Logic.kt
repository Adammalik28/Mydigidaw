package com.example.mydigidaw.data.model

import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.util.toLocalDate
import com.example.mydigidaw.util.toLocalDateTime
import com.example.mydigidaw.util.toMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** Perhitungan kejadian berulang untuk jadwal lokal dan transaksi berulang. */
object Recurrence {
    fun nth(base: LocalDateTime, rule: Repeat, n: Long): LocalDateTime = when (rule) {
        Repeat.NONE -> base
        Repeat.DAILY -> base.plusDays(n)
        Repeat.WEEKLY -> base.plusWeeks(n)
        Repeat.MONTHLY -> base.plusMonths(n)
        Repeat.YEARLY -> base.plusYears(n)
    }

    private fun unitsBetween(a: LocalDateTime, b: LocalDateTime, rule: Repeat): Long = when (rule) {
        Repeat.NONE -> 0
        Repeat.DAILY -> ChronoUnit.DAYS.between(a, b)
        Repeat.WEEKLY -> ChronoUnit.WEEKS.between(a, b)
        Repeat.MONTHLY -> ChronoUnit.MONTHS.between(a, b)
        Repeat.YEARLY -> ChronoUnit.YEARS.between(a, b)
    }

    /** Pasangan (mulai, selesai) setiap kejadian yang beririsan dengan rentang [from, to). */
    fun occurrences(startAt: Long, endAt: Long, rule: Repeat, from: Long, to: Long, limit: Int = 400): List<Pair<Long, Long>> {
        val duration = (endAt - startAt).coerceAtLeast(0)
        if (rule == Repeat.NONE) {
            return if (startAt < to && max(endAt, startAt + 1) > from) listOf(startAt to endAt) else emptyList()
        }
        val base = startAt.toLocalDateTime()
        var n = max(0L, unitsBetween(base, (from - duration).toLocalDateTime(), rule) - 1)
        val result = ArrayList<Pair<Long, Long>>()
        while (result.size < limit) {
            val s = nth(base, rule, n).toMillis()
            if (s >= to) break
            val e = s + duration
            if (max(e, s + 1) > from) result += s to e
            n++
        }
        return result
    }

    /** Waktu mulai kejadian pertama yang >= [time], atau null bila tidak ada lagi. */
    fun nextStartAtOrAfter(startAt: Long, rule: Repeat, time: Long): Long? {
        if (startAt >= time) return startAt
        if (rule == Repeat.NONE) return null
        val base = startAt.toLocalDateTime()
        var n = max(0L, unitsBetween(base, time.toLocalDateTime(), rule) - 1)
        for (i in 0 until 1000) {
            val s = nth(base, rule, n).toMillis()
            if (s >= time) return s
            n++
        }
        return null
    }
}

object HabitLogic {
    /** Senin = bit 0 … Minggu = bit 6. */
    fun isScheduled(habit: HabitEntity, date: LocalDate): Boolean =
        habit.daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

    fun createdDate(habit: HabitEntity): LocalDate = habit.createdAt.toLocalDate()

    /** Awal pelacakan: tanggal dibuat, atau tanggal log paling awal bila pengguna mencentang hari sebelumnya. */
    private fun startDate(habit: HabitEntity, counts: Map<Long, Int>): LocalDate {
        val created = createdDate(habit)
        val earliest = counts.filterValues { it > 0 }.keys.minOrNull()?.let(LocalDate::ofEpochDay) ?: return created
        return minOf(created, earliest)
    }

    /** Rangkaian hari terjadwal yang tuntas berturut-turut sampai hari ini (hari ini tidak memutus streak). */
    fun currentStreak(habit: HabitEntity, counts: Map<Long, Int>, today: LocalDate): Int {
        val created = startDate(habit, counts)
        var day = today
        if (isScheduled(habit, today) && (counts[today.toEpochDay()] ?: 0) < habit.targetPerDay) day = today.minusDays(1)
        var streak = 0
        var guard = 0
        while (guard++ < 3700 && !day.isBefore(created)) {
            if (isScheduled(habit, day)) {
                if ((counts[day.toEpochDay()] ?: 0) >= habit.targetPerDay) streak++ else break
            }
            day = day.minusDays(1)
        }
        return streak
    }

    fun bestStreak(habit: HabitEntity, counts: Map<Long, Int>, today: LocalDate): Int {
        var day = startDate(habit, counts)
        var best = 0
        var run = 0
        var guard = 0
        while (!day.isAfter(today) && guard++ < 3700) {
            if (isScheduled(habit, day)) {
                if ((counts[day.toEpochDay()] ?: 0) >= habit.targetPerDay) {
                    run++
                    best = max(best, run)
                } else if (day != today) {
                    run = 0
                }
            }
            day = day.plusDays(1)
        }
        return best
    }

    /** Persentase hari terjadwal yang tuntas dalam [days] hari terakhir (hari ini dihitung bila sudah tuntas). */
    fun completionRate(habit: HabitEntity, counts: Map<Long, Int>, today: LocalDate, days: Int = 30): Float {
        val created = startDate(habit, counts)
        var scheduled = 0
        var done = 0
        for (i in 0 until days) {
            val d = today.minusDays(i.toLong())
            if (d.isBefore(created)) break
            if (!isScheduled(habit, d)) continue
            val complete = (counts[d.toEpochDay()] ?: 0) >= habit.targetPerDay
            if (d == today && !complete) continue
            scheduled++
            if (complete) done++
        }
        return if (scheduled == 0) 0f else done.toFloat() / scheduled
    }
}

/** Pengaruh sebuah transaksi terhadap saldo gabungan dari kumpulan dompet [accountIds]. */
fun TransactionEntity.effectOn(accountIds: Set<Long>): Long = when (type) {
    TxType.INCOME.name -> if (accountId in accountIds) amount else 0
    TxType.EXPENSE.name -> if (accountId in accountIds) -amount else 0
    else -> (if (toAccountId != null && toAccountId in accountIds) amount else 0) -
        (if (accountId in accountIds) amount else 0)
}
