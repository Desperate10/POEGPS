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
    val isAbon: Boolean
)
