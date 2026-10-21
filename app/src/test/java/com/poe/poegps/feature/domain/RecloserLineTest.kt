package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecloserLineTest {

    private val main = "ПЛ-10 кВ Ватажкове"
    private val tapA = "Відп. від оп. 3 до оп. 2"
    private val tapB = "Відп. від оп. 5 до оп. 3"
    private val poles = listOf(
        LinePoleName(main, "1"), LinePoleName(main, "2"), LinePoleName(main, "3"), LinePoleName(main, "5"),
        LinePoleName(tapA, "3"), LinePoleName(tapA, "1"), LinePoleName(tapA, "2"), LinePoleName(tapA, "2а"),
        LinePoleName(tapB, "5"), LinePoleName(tapB, "1"), LinePoleName(tapB, "3"), LinePoleName(tapB, "3б"),
    )

    @Test
    fun `a pole number the main line has stays with the main line`() {
        assertEquals(main, recloserLine("2", poles))
    }

    @Test
    fun `a pole only a tap has goes with that tap`() {
        assertEquals(tapA, recloserLine("2а", poles))
        assertEquals(tapB, recloserLine("3б", poles))
    }

    @Test
    fun `a pole on no line goes with the main line`() {
        assertEquals(main, recloserLine("99", poles))
    }

    @Test
    fun `an object with only taps on the phone keeps a tap's recloser on it`() {
        assertEquals(tapA, recloserLine("2а", poles.filter { isTapName(it.parentName) }))
        assertNull(recloserLine("99", poles.filter { isTapName(it.parentName) }))
    }
}
