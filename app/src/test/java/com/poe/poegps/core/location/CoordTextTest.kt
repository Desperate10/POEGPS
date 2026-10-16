package com.poe.poegps.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordTextTest {

    @Test
    fun `seven decimals, dot separator, no exponent`() {
        assertEquals("50.2382065", Geo.coordText(50.238206519999984))
        assertEquals("34.2637963", Geo.coordText(34.26379625))
        assertEquals("0.0000010", Geo.coordText(0.000001))
    }

    @Test
    fun `stored text is within a centimetre of the position`() {
        val lat = 50.238206519999984
        val lng = 34.26379625
        val back = Geo.distanceM(lat, lng, Geo.coordText(lat).toDouble(), Geo.coordText(lng).toDouble())
        assertTrue("off by $back m", back < 0.01)
    }
}
