package com.poe.poegps.feature.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GisNamesTest {

    // ~0.00001° ≈ 1 m
    private val poles = listOf(50.0 to 34.0, 50.0 to 34.001, 50.0 to 34.002)

    @Test
    fun `a drawing through two of the line's poles means GIS has it, whatever its name`() {
        for (name in listOf("", "Відп. від оп. 50 до оп. 51", "ПЛ-10 кВ Л-3")) {
            val shape = GisShape(name, listOf(50.00002 to 34.0, 50.0 to 34.001), "len04")
            assertTrue(name, drawnInGis("len04", poles, listOf(shape)))
        }
    }

    @Test
    fun `one pole on a drawing (a tap's junction) is not enough`() {
        val main = GisShape("ПЛ", listOf(50.0 to 34.0, 50.01 to 34.0), "len04")
        assertFalse(drawnInGis("len04", poles, listOf(main)))
    }

    @Test
    fun `only drawings of the line's own layer count`() {
        val cable = GisShape("КЛ", listOf(50.0 to 34.0, 50.0 to 34.001), "lek04")
        assertFalse(drawnInGis("len04", poles, listOf(cable)))
    }
}
