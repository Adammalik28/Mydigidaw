package com.example.mydigidaw.ui.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistTest {
    @Test
    fun parse_readsCheckedUncheckedAndBulletLines() {
        val items = Checklist.parse("[ ] susu\n[x] roti\n[X] telur\n- kopi\n• teh")
        assertEquals(
            listOf(
                ChecklistItem("susu", false),
                ChecklistItem("roti", true),
                ChecklistItem("telur", true),
                ChecklistItem("kopi", false),
                ChecklistItem("teh", false),
            ),
            items,
        )
    }

    @Test
    fun serialize_isTheInverseOfParse() {
        val text = "[ ] susu\n[x] roti dan selai, 2 bungkus"
        assertEquals(text, Checklist.serialize(Checklist.parse(text)))
    }

    @Test
    fun plainTextConversion_dropsBlankLines() {
        assertEquals("[ ] a\n[ ] b", Checklist.fromPlain("a\n\n  b  \n"))
        assertEquals("a\nb", Checklist.toPlain("[ ] a\n[ ] \n[x] b"))
        assertEquals("[ ] ", Checklist.fromPlain(""))
        assertTrue(Checklist.parse("").isEmpty())
    }

    @Test
    fun shareText_usesCheckboxSymbols() {
        assertEquals("☐ susu\n☑ roti", Checklist.toShareText("[ ] susu\n[x] roti\n[ ] "))
    }
}
