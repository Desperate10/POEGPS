package com.poe.poegps.feature.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GisMergeTest {

    private fun gis(vararg names: String) = names.mapIndexed { i, n -> GisPoint(n, 50.0 + i * 0.001, 34.0) }
    private fun local(vararg names: String) = names.mapIndexed { i, n -> LocalPole(100 + i, n) }

    /** The merged line as names: phone poles as "phone:<name>", GIS ones as their name. */
    private fun names(merged: List<MergedPole>?, local: List<LocalPole>) = merged?.map { m ->
        when (m) {
            is MergedPole.Local -> "phone:" + local.first { it.id == m.id }.name
            is MergedPole.Gis -> m.point.name
        }
    }

    @Test
    fun `phone poles take their GIS places and the missing ones are added`() {
        val l = local("1", "2", "3")
        assertEquals(
            listOf("phone:1", "phone:2", "phone:3", "4", "5"),
            names(mergeWithGis(l, gis("1", "2", "3", "4", "5")), l),
        )
    }

    @Test
    fun `a pole the crew added stays after the pole it followed`() {
        val l = local("1", "2", "2б", "3")
        assertEquals(
            listOf("phone:1", "phone:2", "phone:2б", "phone:3", "4"),
            names(mergeWithGis(l, gis("1", "2", "3", "4")), l),
        )
    }

    @Test
    fun `GIS order wins over the phone order`() {
        val l = local("3", "1")
        assertEquals(listOf("phone:1", "2", "phone:3"), names(mergeWithGis(l, gis("1", "2", "3")), l))
    }

    @Test
    fun `names match ignoring case and spaces`() {
        val l = local(" 13Б")
        assertEquals(listOf("12", "phone: 13Б", "14"), names(mergeWithGis(l, gis("12", "13б", "14")), l))
    }

    @Test
    fun `nothing missing means no offer`() {
        assertNull(mergeWithGis(local("1", "2"), gis("1", "2")))
    }

    @Test
    fun `no shared name means no offer`() {
        assertNull(mergeWithGis(local("101", "102"), gis("1", "2")))
    }
}
