package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LineBranchesTest {

    private fun pole(line: String, name: String, at: Int, checked: Boolean = false, unsent: Boolean = false) =
        BranchPole(line, name, "50.$at", "34.$at", checked, unsent)

    private val main = "ПЛ-0,4 кВ Л-1"

    @Test
    fun `taps go under the line they hang from, in junction order, labelled by their poles`() {
        val poles = listOf(
            pole(main, "1", 1), pole(main, "2", 2), pole(main, "3", 3),
            // GIS calls it "від оп. 29": the poles say 3
            pole("Відп. від оп. 29 до оп. 31", "3", 3), pole("Відп. від оп. 29 до оп. 31", "4", 4),
            pole("Відп. від оп. 1 до оп. 8", "1", 1), pole("Відп. від оп. 1 до оп. 8", "8", 8, checked = true, unsent = true),
            pole("Відп. від оп. 8 до оп. 9", "8", 8), pole("Відп. від оп. 8 до оп. 9", "9", 9),
        )
        assertEquals(
            listOf(
                Branch(main, 0, "Магістраль", 3, 0, 0, true),
                Branch("Відп. від оп. 1 до оп. 8", 1, "від оп. 1 → 8", 2, 1, 1, false),
                Branch("Відп. від оп. 8 до оп. 9", 2, "від оп. 8 → 9", 2, 0, 0, false),
                Branch("Відп. від оп. 29 до оп. 31", 1, "від оп. 3 → 4", 2, 0, 0, false),
            ),
            branchTree(main, poles),
        )
    }

    @Test
    fun `a junction moved on the tap only, or a tap on nothing, stays at the top`() {
        val poles = listOf(
            pole(main, "1", 1),
            pole("Відп. А", "1", 7), pole("Відп. А", "2", 8),
            pole("Відп. Б", "5", 5),
        )
        assertEquals(
            listOf(main to 0, "Відп. А" to 0, "Відп. Б" to 0),
            branchTree(main, poles).map { it.line to it.depth },
        )
        assertEquals("від оп. 5", branchTree(main, poles).last().label)
    }

    @Test
    fun `a main line not on the phone yet is listed empty`() {
        assertEquals(Branch(main, 0, "Магістраль", 0, 0, 0, true), branchTree(main, emptyList()).single())
    }
}
