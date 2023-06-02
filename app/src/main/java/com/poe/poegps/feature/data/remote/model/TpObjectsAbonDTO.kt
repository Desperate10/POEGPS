package com.poe.poegps.feature.data.remote.model

data class TpObjectsAbonDTO(
    val tplnr: String,
    val pltxt: String,
    val lng: String?,
    val lat: String?,
    val isAbon: Boolean = true
)
