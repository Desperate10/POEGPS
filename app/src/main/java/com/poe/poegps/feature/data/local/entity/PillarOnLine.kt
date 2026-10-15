package com.poe.poegps.feature.data.local.entity

/** A GIS pole (pillars04/pillars10) with the name of the line it belongs to; not a table. */
data class PillarOnLine(
    val id: Int,
    val tplnr: String,
    val name: String,
    val wire: String,
    val lat: String,
    val lng: String,
    val lineName: String?,
)
