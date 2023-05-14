package com.poe.poegps.feature.presentation.model

data class OprDisplayable(
    val tplnr: String,
    val name: String,
    val lat: String?,
    val lng: String?,
    val wire: String?
)