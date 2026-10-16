package com.poe.poegps.feature.data.local.entity

/** What makes a GIS pole (pillars04/pillars10) the same pole on a repeated download; not a table. */
data class PoleKey(
    val tplnr: String,
    val name: String,
)
