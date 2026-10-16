package com.poe.poegps.feature.domain.model

data class Pillar(
    val id: Int,
    val tplnr: String,
    val name: String,
    val parentName: String,
    val category: String,
    val wire: String,
    val pillarType: String,
    val lng: String,
    val lat: String,
    val isAbon: Boolean,
    val recloser: String,
    val isSent: Boolean = false,
    val fromGis: Boolean = false,
    /** Loaded from GIS; stays true when the pole is moved, so it can be put back as GIS has it. */
    val gisOrigin: Boolean = false,
)
