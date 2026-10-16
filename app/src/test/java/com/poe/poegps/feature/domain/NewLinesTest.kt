package com.poe.poegps.feature.domain

import com.poe.poegps.feature.domain.model.UploadLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewLinesTest {

    private fun line(name: String, tplnr: String = "PO-F-08-L0222", type: String = "PILLAR",
                     category: String = "0,4 кВ", abon: Boolean = false, poles: Int = 5) =
        UploadLine(tplnr, name, type, category, abon, poles)

    private val gis = setOf(
        GisLineKey("PO-F-08-L0222", "ПЛ-0,4 кВ КТП-79 л-2", "len04"),
        GisLineKey("PO-F-08-L0029", "ПЛ-10 кВ Ф-1", "len10"),
    )

    @Test
    fun `layer mirrors the server's table choice`() {
        assertEquals("len04", gisLayerOf("PILLAR", "0,4 кВ", false))
        assertEquals("len10", gisLayerOf("PILLAR", "10 кВ", false))
        assertEquals("abon_lek10", gisLayerOf("KL", "10 кВ", true))
        assertNull(gisLayerOf("TP", "0,4 кВ", false))
        assertNull(gisLayerOf("PILLAR", "35 кВ", false))
    }

    @Test
    fun `a line GIS draws under the same object and name is not new`() {
        assertEquals(emptyList<UploadLine>(), linesNewToGis(listOf(line("ПЛ-0,4 кВ КТП-79 л-2")), gis))
    }

    @Test
    fun `a tap with a name GIS doesn't have is new`() {
        val tap = line("відп. від оп. 12")
        assertEquals(listOf(tap), linesNewToGis(listOf(tap), gis))
    }

    @Test
    fun `the name must match exactly`() {
        val typo = line("ПЛ-0,4 кВ КТП-79  л-2")
        assertEquals(listOf(typo), linesNewToGis(listOf(typo), gis))
    }

    @Test
    fun `same name in another object or layer is new`() {
        val other = line("ПЛ-0,4 кВ КТП-79 л-2", tplnr = "PO-F-08-L0999")
        val abon = line("ПЛ-0,4 кВ КТП-79 л-2", abon = true)
        assertEquals(listOf(other, abon), linesNewToGis(listOf(other, abon), gis))
    }

    @Test
    fun `lines the server won't draw are not listed`() {
        assertEquals(emptyList<UploadLine>(), linesNewToGis(listOf(line("нова", poles = 1), line("ТП", type = "TP")), gis))
    }
}
