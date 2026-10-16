package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GisTapsTest {

    // pole 74 of the main line at 50.0, 34.0; ~0.00001° ≈ 1 m
    private val main = GisShape("ПЛ-10 кВ Місто", listOf(50.0 to 33.99, 50.0 to 34.0, 50.0 to 34.01))
    private val tapByName = GisShape("Відп. від оп. 74 до оп. 157", listOf(50.1 to 34.1, 50.2 to 34.2))
    private val tapByEnd = GisShape("відп.на ТП-48", listOf(50.00002 to 34.0, 50.01 to 34.0))
    private val tapElsewhere = GisShape("Відп. від оп. 12 до оп. 13", listOf(49.0 to 33.0, 49.1 to 33.1))
    private val unnamed = GisShape("", listOf(50.0 to 34.0, 50.01 to 34.01))
    private val shapes = listOf(main, tapByName, tapByEnd, tapElsewhere, unnamed)

    @Test
    fun `taps named after the pole come first, then those drawn from it`() {
        assertEquals(
            listOf(GisTap("Відп. від оп. 74 до оп. 157", "157"), GisTap("відп.на ТП-48", null)),
            gisTapsAt("74", 50.0, 34.0, shapes, exclude = setOf("ПЛ-10 кВ Місто")),
        )
    }

    @Test
    fun `pole letters match in any case`() {
        val tap = GisShape("Відп. від оп. 58Б до оп. 145", listOf(49.0 to 33.0, 49.1 to 33.1))
        assertEquals(listOf(GisTap(tap.name, "145")), gisTapsAt("58б", null, null, listOf(tap), emptySet()))
    }

    @Test
    fun `a pole without coordinates only finds taps by name`() {
        assertEquals(listOf("Відп. від оп. 74 до оп. 157"), gisTapsAt("74", null, null, shapes, setOf("ПЛ-10 кВ Місто")).map { it.name })
    }

    @Test
    fun `the line being edited and pieces of one tap are listed once or not at all`() {
        val piece = tapByName.copy(vertices = listOf(50.3 to 34.3, 50.4 to 34.4))
        val found = gisTapsAt("74", 50.0, 34.0, shapes + piece, exclude = setOf("ПЛ-10 кВ Місто"))
        assertEquals(1, found.count { it.name == tapByName.name })
        assertEquals(0, found.count { it.name == main.name })
    }

    @Test
    fun `from pole 7 is not pole 74`() {
        val tap = GisShape("Відп. від оп. 7 до оп. 8", listOf(49.0 to 33.0, 49.1 to 33.1))
        assertEquals(emptyList<GisTap>(), gisTapsAt("74", null, null, listOf(tap), emptySet()))
    }
}
