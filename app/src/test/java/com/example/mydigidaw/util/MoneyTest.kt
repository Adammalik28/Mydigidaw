package com.example.mydigidaw.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MoneyTest {
    /** Spasi tak terputus diganti spasi biasa agar perbandingan mudah dibaca. */
    private fun String.plain() = replace(' ', ' ')

    @Test
    fun format_usesIndonesianGrouping() {
        assertEquals("Rp 1.250.000", Money.format(1_250_000).plain())
        assertEquals("Rp 0", Money.format(0).plain())
    }

    @Test
    fun format_signs() {
        assertEquals("−Rp 25.000", Money.format(-25_000).plain())
        assertEquals("+Rp 25.000", Money.format(25_000, withSign = true).plain())
        assertEquals("Rp 0", Money.format(0, withSign = true).plain())
    }

    @Test
    fun compact_forChartAxes() {
        assertEquals("999", Money.compact(999))
        assertEquals("350 rb", Money.compact(350_000))
        assertEquals("1,2 jt", Money.compact(1_200_000))
        assertEquals("150 jt", Money.compact(150_000_000))
        assertEquals("3 M", Money.compact(3_000_000_000))
        assertEquals("−2,5 rb", Money.compact(-2_500))
    }

    @Test
    fun masked_hidesDigits() {
        assertEquals("Rp •••••", Money.masked().plain())
    }

    @Test
    fun relativeDay_labels() {
        val today = LocalDate.of(2026, 10, 15)
        assertEquals("Hari ini", Dates.relativeDay(today, today))
        assertEquals("Besok", Dates.relativeDay(today.plusDays(1), today))
        assertEquals("Kemarin", Dates.relativeDay(today.minusDays(1), today))
    }

    @Test
    fun greeting_followsTimeOfDay() {
        assertEquals("Selamat pagi", Dates.greeting(6))
        assertEquals("Selamat siang", Dates.greeting(12))
        assertEquals("Selamat sore", Dates.greeting(16))
        assertEquals("Selamat malam", Dates.greeting(22))
    }
}
