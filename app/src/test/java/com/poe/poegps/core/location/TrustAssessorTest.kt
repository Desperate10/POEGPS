package com.poe.poegps.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustAssessorTest {

    private val t0 = 1_000_000L
    private val clock = 1_760_000_000_000L

    private fun sample(
        sec: Int,
        lat: Double? = 49.5900,
        lng: Double? = 34.5500,
        accuracy: Float? = 3f,
        mock: Boolean = false,
        used: Int = 12,
        visible: Int = 20,
        cn0: List<Float> = listOf(28f, 33f, 41f, 37f, 25f, 44f, 31f, 39f),
        agc: Double? = null,
        fixTimeOffsetMs: Long = 0,
    ) = GnssSample(
        elapsedMs = t0 + sec * 1000L,
        lat = lat, lng = lng, accuracyM = accuracy,
        fixTimeMs = if (lat == null) null else clock + sec * 1000L + fixTimeOffsetMs,
        wallClockMs = clock + sec * 1000L,
        isMock = mock,
        satellitesUsed = used, satellitesVisible = visible,
        cn0 = cn0, agcDb = agc,
    )

    @Test
    fun `ordinary walking along a line is reliable`() {
        val a = TrustAssessor()
        var state = TrustState()
        for (i in 0..30) state = a.update(sample(i, lat = 49.5900 + i * 0.00001))
        assertEquals(TrustLevel.RELIABLE, state.level)
    }

    @Test
    fun `mock location is spoofing and locks capture`() {
        val state = TrustAssessor().update(sample(0, mock = true))
        assertEquals(TrustLevel.SPOOFING, state.level)
        assertTrue(state.level.locksCapture)
    }

    @Test
    fun `a kilometre in two seconds is spoofing, and the verdict is held`() {
        val a = TrustAssessor()
        a.update(sample(0))
        assertEquals(TrustLevel.SPOOFING, a.update(sample(2, lat = 49.6000)).level)
        // clean samples right after must not unlock capture
        assertEquals(TrustLevel.SPOOFING, a.update(sample(5, lat = 49.6000)).level)
        assertEquals(TrustLevel.RELIABLE, a.update(sample(2 + 61, lat = 49.6000)).level)
    }

    @Test
    fun `a jump within the accuracy of a cold fix is not spoofing`() {
        val a = TrustAssessor()
        a.update(sample(0, accuracy = 400f))
        assertEquals(TrustLevel.RELIABLE, a.update(sample(1, lat = 49.5930, accuracy = 4f)).level)
    }

    @Test
    fun `identical strong signals on every satellite are suspicious`() {
        val state = TrustAssessor().update(sample(0, cn0 = List(10) { 47f }))
        assertEquals(TrustLevel.SUSPICIOUS, state.level)
        assertFalse(state.level.locksCapture)
    }

    @Test
    fun `GNSS clock far from the phone clock is suspicious`() {
        val state = TrustAssessor().update(sample(0, fixTimeOffsetMs = 10 * 60_000L))
        assertEquals(TrustLevel.SUSPICIOUS, state.level)
    }

    @Test
    fun `losing a good fix while satellites are heard but drowned is jamming`() {
        val a = TrustAssessor()
        a.update(sample(0))
        val weak = List(8) { 12f }
        var state = TrustState()
        for (i in 1..25) state = a.update(sample(i, lat = null, lng = null, accuracy = null, used = 0, visible = 8, cn0 = weak))
        assertEquals(TrustLevel.JAMMING, state.level)
    }

    @Test
    fun `simply losing the sky under trees is not jamming`() {
        val a = TrustAssessor()
        a.update(sample(0))
        var state = TrustState()
        for (i in 1..25) state = a.update(sample(i, lat = null, lng = null, accuracy = null, used = 0, visible = 2, cn0 = listOf(15f, 18f)))
        assertEquals(TrustLevel.RELIABLE, state.level)
    }

    @Test
    fun `a collapse in receiver gain is jamming`() {
        val a = TrustAssessor()
        for (i in 0 until 20) a.update(sample(i, agc = 30.0))
        assertEquals(TrustLevel.JAMMING, a.update(sample(21, accuracy = 30f, agc = 12.0)).level)
    }

    @Test
    fun `far outside the branch is suspicious, just over the border is not`() {
        val square: Area = listOf(listOf(listOf(49.0 to 34.0, 49.0 to 35.0, 50.0 to 35.0, 50.0 to 34.0)))
        // a fresh assessor each time: hopping between these points would rightly count as a jump
        assertEquals(TrustLevel.RELIABLE, TrustAssessor(square).update(sample(0, lat = 49.5, lng = 34.5)).level)
        assertEquals(TrustLevel.RELIABLE, TrustAssessor(square).update(sample(0, lat = 50.005, lng = 34.5)).level)
        assertEquals(TrustLevel.SUSPICIOUS, TrustAssessor(square).update(sample(0, lat = 50.05, lng = 34.5)).level)
    }

    @Test
    fun `geometry helpers`() {
        val square: Area = listOf(listOf(listOf(49.0 to 34.0, 49.0 to 35.0, 50.0 to 35.0, 50.0 to 34.0)))
        assertTrue(Geo.contains(square, 49.5, 34.5))
        assertFalse(Geo.contains(square, 50.5, 34.5))
        assertEquals(11_120.0, Geo.distanceM(49.0, 34.0, 49.1, 34.0), 50.0)
        assertEquals(5_560.0, Geo.distanceToEdgeM(square, 50.05, 34.5), 100.0)
    }
}
