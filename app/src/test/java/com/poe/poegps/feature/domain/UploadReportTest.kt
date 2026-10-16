package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadReportTest {

    private val tplnr = "PO-F-08-L0222"
    private val tap = "Відп. від оп. 50 до оп. 999"
    private val main = "ПЛ-0,4 КВ КТП-79 л-2 с.В.Павлівка"

    // the real answer from the 16.10 test (two poles of the tap 60 km away)
    private val message = "Не прийнято (перевірте координати): " +
        "$tap оп. 999: точка далі ніж 10 км від меж філії PO-F-08 — можлива підміна GPS; " +
        "$tap оп. 998: точка далі ніж 10 км від меж філії PO-F-08 — можлива підміна GPS. Вивантажено успішно! "

    @Test
    fun `only the line the server named is refused, with its own reasons`() {
        val parsed = UploadReport.parse(message)
        assertEquals(
            "оп. 999: точка далі ніж 10 км від меж філії PO-F-08 — можлива підміна GPS; " +
                "оп. 998: точка далі ніж 10 км від меж філії PO-F-08 — можлива підміна GPS",
            UploadReport.reasonFor(tap, tplnr, setOf(tplnr), parsed),
        )
        assertNull(UploadReport.reasonFor(main, tplnr, setOf(tplnr), parsed))
    }

    @Test
    fun `a refusal after the save names the object, so every line of it`() {
        val parsed = UploadReport.parse("Вивантажено успішно! $tplnr лінію не змінено: опори не збігаються з лінією в ГІС; ")
        assertEquals(
            "лінію не змінено: опори не збігаються з лінією в ГІС",
            UploadReport.reasonFor(main, tplnr, setOf(tplnr), parsed),
        )
    }

    @Test
    fun `objects not in the refused list are accepted`() {
        assertNull(UploadReport.reasonFor(main, "PO-F-08-L0001", setOf(tplnr), UploadReport.parse(message)))
    }

    @Test
    fun `an unreadable message still refuses the whole object`() {
        assertEquals("", UploadReport.reasonFor(main, tplnr, setOf(tplnr), UploadReport.parse("помилка")))
    }
}
