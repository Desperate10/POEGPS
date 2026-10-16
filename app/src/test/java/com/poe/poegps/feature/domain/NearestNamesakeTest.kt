package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearestNamesakeTest {

    // pole "1" of the main line and pole "1" of a tap 500 m away; ~0.00001° ≈ 1 m
    private val gis = listOf(
        GisPoint("1", 50.0, 34.0),
        GisPoint("2", 50.0005, 34.0),
        GisPoint("1", 50.0045, 34.0),
    )

    @Test
    fun `a moved pole finds the namesake it was moved from`() {
        assertEquals(0, nearestNamesake("1", 50.00003, 34.0, gis))
        assertEquals(2, nearestNamesake("1", 50.0044, 34.0, gis))
    }

    @Test
    fun `names match ignoring case and spaces`() {
        val withLetter = listOf(GisPoint("13Б", 50.0, 34.0))
        assertEquals(0, nearestNamesake(" 13б", 50.0, 34.0, withLetter))
    }

    @Test
    fun `without coordinates only a single namesake counts`() {
        assertEquals(1, nearestNamesake("2", null, null, gis))
        assertNull(nearestNamesake("1", null, null, gis))
    }

    @Test
    fun `no namesake, no GIS position`() {
        assertNull(nearestNamesake("999", 50.0, 34.0, gis))
    }
}
