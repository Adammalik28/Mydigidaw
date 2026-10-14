package com.example.mydigidaw.ui.components

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsTransformationTest {
    @Test
    fun insertsDotsEveryThreeDigits() {
        assertEquals("1.250.000", ThousandsTransformation.filter(AnnotatedString("1250000")).text.text)
        assertEquals("999", ThousandsTransformation.filter(AnnotatedString("999")).text.text)
        assertEquals("", ThousandsTransformation.filter(AnnotatedString("")).text.text)
    }

    @Test
    fun cursorMapping_roundTripsForEveryPosition() {
        val digits = "123456789"
        val mapping = ThousandsTransformation.filter(AnnotatedString(digits)).offsetMapping
        for (i in 0..digits.length) {
            assertEquals("posisi $i", i, mapping.transformedToOriginal(mapping.originalToTransformed(i)))
        }
        assertEquals(11, mapping.originalToTransformed(digits.length))
    }
}
