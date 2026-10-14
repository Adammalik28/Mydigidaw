package com.example.mydigidaw.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

val LocaleID: Locale = Locale.forLanguageTag("id-ID")

/** Format angka & uang dalam gaya Indonesia (Rp 1.250.000). */
object Money {
    private const val NBSP = ' '

    fun format(amount: Long, withSign: Boolean = false): String {
        val body = "Rp$NBSP" + NumberFormat.getIntegerInstance(LocaleID).format(abs(amount))
        return when {
            amount < 0 -> "−$body"
            withSign && amount > 0 -> "+$body"
            else -> body
        }
    }

    fun digits(amount: Long): String = NumberFormat.getIntegerInstance(LocaleID).format(amount)

    /** Bentuk ringkas untuk sumbu grafik: 350 rb, 1,2 jt, 3 M. */
    fun compact(amount: Long): String {
        val a = abs(amount).toDouble()
        val sign = if (amount < 0) "−" else ""
        val (value, suffix) = when {
            a >= 1e12 -> a / 1e12 to " T"
            a >= 1e9 -> a / 1e9 to " M"
            a >= 1e6 -> a / 1e6 to " jt"
            a >= 1e3 -> a / 1e3 to " rb"
            else -> a to ""
        }
        val nf = NumberFormat.getNumberInstance(LocaleID).apply {
            maximumFractionDigits = if (suffix.isEmpty() || value >= 100) 0 else 1
        }
        return sign + nf.format(value) + suffix
    }

    /** Menyamarkan nominal ketika mode privasi aktif. */
    fun masked(): String = "Rp$NBSP•••••"
}

object Dates {
    val zone: ZoneId get() = ZoneId.systemDefault()

    private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LocaleID)
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", LocaleID)
    private val mediumDate = DateTimeFormatter.ofPattern("d MMM yyyy", LocaleID)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", LocaleID)
    private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEE, d MMM", LocaleID)
    private val weekdayFull = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", LocaleID)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", LocaleID)
    private val monthShort = DateTimeFormatter.ofPattern("MMM", LocaleID)
    private val monthShortYear = DateTimeFormatter.ofPattern("MMM yy", LocaleID)
    private val time = DateTimeFormatter.ofPattern("HH:mm", LocaleID)

    fun full(date: LocalDate): String = fullDate.format(date)
    fun long(date: LocalDate): String = longDate.format(date)
    fun medium(date: LocalDate): String = mediumDate.format(date)
    fun dayMonth(date: LocalDate): String = dayMonth.format(date)
    fun weekdayDayMonth(date: LocalDate): String = weekdayDayMonth.format(date)
    fun weekdayFull(date: LocalDate): String = weekdayFull.format(date)
    fun monthYear(month: YearMonth): String = monthYear.format(month)
    fun monthShort(month: YearMonth): String = monthShort.format(month)
    fun monthShortYear(month: YearMonth): String = monthShortYear.format(month)
    fun time(t: LocalTime): String = time.format(t)
    fun time(millis: Long): String = time.format(millis.toLocalDateTime())
    fun dateTime(millis: Long): String {
        val dt = millis.toLocalDateTime()
        return "${mediumDate.format(dt.toLocalDate())}, ${time.format(dt)}"
    }

    /** "Hari ini", "Besok", "Kemarin", atau tanggal singkat. */
    fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Hari ini"
        today.plusDays(1) -> "Besok"
        today.minusDays(1) -> "Kemarin"
        else -> if (date.year == today.year) weekdayDayMonth(date) else medium(date)
    }

    fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
        in 4..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..17 -> "Selamat sore"
        else -> "Selamat malam"
    }

    /** Label singkat hari dalam seminggu, dimulai Senin. */
    val weekdayShort = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
}

fun Long.toLocalDateTime(): LocalDateTime = Instant.ofEpochMilli(this).atZone(Dates.zone).toLocalDateTime()
fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(Dates.zone).toLocalDate()
fun LocalDate.startMillis(): Long = atStartOfDay(Dates.zone).toInstant().toEpochMilli()
fun LocalDate.endMillis(): Long = plusDays(1).startMillis()
fun LocalDateTime.toMillis(): Long = atZone(Dates.zone).toInstant().toEpochMilli()
fun YearMonth.startMillis(): Long = atDay(1).startMillis()
fun YearMonth.endMillis(): Long = plusMonths(1).atDay(1).startMillis()

/** DatePicker Material bekerja dengan milidetik UTC tengah malam. */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun utcMillisToLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
