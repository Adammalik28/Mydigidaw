package com.example.mydigidaw.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareParserTest {
    @Test
    fun linkWithSubject_usesSubjectAsTitle() {
        val shared = ShareParser.parse("https://www.youtube.com/watch?v=abc", "Tutorial memasak rendang")!!
        assertEquals("Tutorial memasak rendang", shared.title)
        assertEquals("https://www.youtube.com/watch?v=abc", shared.body)
        assertNull(shared.amount)
    }

    @Test
    fun bareLink_usesItsDomainAsTitle() {
        val shared = ShareParser.parse("https://www.tokopedia.com/produk/123")!!
        assertEquals("tokopedia.com", shared.title)
    }

    @Test
    fun bankSms_detectsTheAmount() {
        val shared = ShareParser.parse("Pembayaran Rp25.000,00 di INDOMARET berhasil")!!
        assertEquals(25_000L, shared.amount)
        assertEquals("Pembayaran Rp25.000,00 di INDOMARET berhasil", shared.title)
        assertEquals("judul sama dengan isi, isi dikosongkan", "", shared.body)
    }

    @Test
    fun amountFormats() {
        assertEquals(1_250_000L, ShareParser.findAmount("Transfer IDR 1,250,000 ke Budi"))
        assertEquals(50_000L, ShareParser.findAmount("tagihan rp 50000"))
        assertEquals(3_500_000L, ShareParser.findAmount("Total: Rp. 3.500.000"))
        assertNull(ShareParser.findAmount("tidak ada nominal 2026"))
        assertNull(ShareParser.findAmount("Rp0"))
    }

    @Test
    fun multiLineText_takesFirstLineAsTitle() {
        val shared = ShareParser.parse("\n  Daftar belanja \nsusu\nroti")!!
        assertEquals("Daftar belanja", shared.title)
        assertEquals("Daftar belanja \nsusu\nroti", shared.body)
    }

    @Test
    fun longTitles_areShortenedWithEllipsis() {
        val shared = ShareParser.parse("a".repeat(200))!!
        assertEquals(80, shared.title.length)
        assertTrue(shared.title.endsWith("…"))
        assertEquals(200, shared.body.length)
    }

    @Test
    fun emptyShares_areIgnored() {
        assertNull(ShareParser.parse(null))
        assertNull(ShareParser.parse("   ", "  "))
    }
}
