package com.poe.poegps.feature.presentation.screens.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class NextPoleNamesTest {

    @Test
    fun `empty line starts at 1`() {
        assertEquals(listOf("1", "2"), nextPoleNames(null, emptySet()))
    }

    @Test
    fun `plain number suggests next, letter, slash and the one after`() {
        assertEquals(listOf("14", "13а", "13/1", "15"), nextPoleNames("13", setOf("13")))
    }

    @Test
    fun `keeps the prefix of the last name`() {
        assertEquals("Опора 14", nextPoleNames("Опора 13", setOf("Опора 13")).first())
    }

    @Test
    fun `after a branch pole the branch number goes on`() {
        assertEquals("13/2", nextPoleNames("13/1", setOf("13", "13/1")).first())
    }

    @Test
    fun `names already on the line are skipped`() {
        assertEquals(listOf("14", "13/1", "15"), nextPoleNames("13а", setOf("13", "13а")))
    }

    @Test
    fun `no number means no guess`() {
        assertEquals(emptyList<String>(), nextPoleNames("КТП", setOf("КТП")))
    }
}
