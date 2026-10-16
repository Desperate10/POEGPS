package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GisNamesTest {

    // ~0.00001° ≈ 1 m
    private val poles = listOf(50.0 to 34.0, 50.0 to 34.001, 50.0 to 34.002)
    private val sameLine = GisShape("Відп. від оп. 50 до оп. 51", listOf(50.00002 to 34.0, 50.0 to 34.001, 50.0 to 34.002), "len04")
    private val touchesOnce = GisShape("Відп. від оп. 50 до оп. 70", listOf(50.0 to 34.0, 50.01 to 34.0), "len04")
    private val otherLayer = GisShape("КЛ-0,4 кВ", listOf(50.0 to 34.0, 50.0 to 34.001), "lek04")
    private val unnamed = GisShape("", listOf(50.0 to 34.0, 50.0 to 34.001), "len04")

    private fun names(shapes: List<GisShape>, used: Set<String> = emptySet()) =
        gisNameCandidates("Відп. від оп. 50 до оп. 999", "len04", poles, shapes, used).map { it.name to it.polesOnIt }

    @Test
    fun `a drawing through the line's poles is offered`() {
        assertEquals(listOf("Відп. від оп. 50 до оп. 51" to 3), names(listOf(sameLine, touchesOnce, otherLayer, unnamed)))
    }

    @Test
    fun `pieces of one drawing count together`() {
        val a = GisShape("Л-2", listOf(50.0 to 34.0, 50.0 to 33.99), "len04")
        val b = GisShape("Л-2", listOf(50.0 to 34.002, 50.0 to 34.01), "len04")
        assertEquals(listOf("Л-2" to 2), names(listOf(a, b)))
    }

    @Test
    fun `names another line on the phone uses are not offered`() {
        assertEquals(emptyList<Pair<String, Int>>(), names(listOf(sameLine), used = setOf(sameLine.name)))
    }

    @Test
    fun `the best match comes first`() {
        val two = GisShape("Б", listOf(50.0 to 34.0, 50.0 to 34.001), "len04")
        assertEquals(listOf(sameLine.name to 3, "Б" to 2), names(listOf(two, sameLine)))
    }
}
