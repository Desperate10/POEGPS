package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineOrderingTest {

    // poles ~11 m apart going north; GIS stores them in no particular order
    private fun pole(name: String, step: Int, east: Int = 0) = GisPoint(name, 50.0 + step * 0.0001, 34.0 + east * 0.0001)
    private fun at(p: GisPoint, jitterM: Double = 0.0) = (p.lat + jitterM / 111_320.0) to p.lng

    private val p1 = pole("1", 1)
    private val p2 = pole("2", 2)
    private val p3 = pole("3", 3)
    private val p4 = pole("4", 4)
    private val t1 = pole("1", 3, east = 1)   // a tap pole that reuses the name "1"
    private val t2 = pole("2", 3, east = 2)

    @Test
    fun `main line order comes from its geometry, not from the table`() {
        val shapes = listOf(GisShape("ПЛ-0,4 кВ Л-1", listOf(at(p1), at(p2), at(p3), at(p4))))
        val result = LineOrdering.order("ПЛ-0,4 кВ Л-1", listOf(p3, p1, p4, p2), shapes)
        assertTrue(result.fromShape)
        assertEquals(listOf("1", "2", "3", "4"), result.poles.map { it.name })
    }

    @Test
    fun `vertices drawn a little off the poles still match`() {
        val shapes = listOf(GisShape("Л-1", listOf(at(p1, 1.5), at(p2, -2.0), at(p3, 2.5))))
        assertEquals(listOf("1", "2", "3"), LineOrdering.order("Л-1", listOf(p1, p2, p3), shapes).poles.map { it.name })
    }

    @Test
    fun `tap poles are not loaded into the main line`() {
        val shapes = listOf(
            GisShape("Л-1", listOf(at(p1), at(p2), at(p3), at(p4))),
            GisShape("Відп. від оп. 3 до оп. 1", listOf(at(p3), at(t1), at(t2))),
        )
        val result = LineOrdering.order("Л-1", listOf(p1, t1, p2, t2, p3, p4), shapes)
        assertEquals(listOf(p1, p2, p3, p4), result.poles)
    }

    @Test
    fun `line name differing only in case still finds the geometry`() {
        val shapes = listOf(GisShape("ПЛ-0,4 кВ Л-1", listOf(at(p1), at(p2))))
        assertTrue(LineOrdering.order("ПЛ-0,4 КВ Л-1", listOf(p2, p1), shapes).fromShape)
    }

    @Test
    fun `a main line drawn under another name or none is found by its poles`() {
        for (drawn in listOf("", "ПЛ-10 кВ Л-3 (як у ГІС)")) {
            val shapes = listOf(
                GisShape(drawn, listOf(at(p1), at(p2), at(p3), at(p4))),
                GisShape("Відп. від оп. 3 до оп. 1", listOf(at(p3), at(t1), at(t2))),
            )
            val result = LineOrdering.order("Л-1", listOf(p4, t1, p2, t2, p3, p1), shapes)
            assertTrue(result.fromShape)
            assertEquals(listOf(p1, p2, p3, p4), result.poles)
        }
    }

    @Test
    fun `a tap GIS doesn't draw yet is not given the main line's poles`() {
        val shapes = listOf(GisShape("", listOf(at(p1), at(p2), at(p3), at(p4))))
        val result = LineOrdering.order("Відп. від оп. 3 до оп. 999", listOf(p1, p2, p3, p4), shapes)
        assertFalse(result.fromShape)
    }

    @Test
    fun `two drawings through as many poles give no main line`() {
        val shapes = listOf(
            GisShape("", listOf(at(p1), at(p2))),
            GisShape("", listOf(at(p3), at(p4))),
        )
        val result = LineOrdering.order("Л-1", listOf(p1, p2, p3, p4), shapes)
        assertFalse(result.fromShape)
    }

    @Test
    fun `no geometry at all orders by number with letters after their number`() {
        val poles = listOf("10", "2", "13а", "1", "13", "13/1").map { GisPoint(it, 50.0, 34.0) }
        assertEquals(listOf("1", "2", "10", "13", "13/1", "13а"), LineOrdering.order("Л-1", poles, emptyList()).poles.map { it.name })
    }
}
