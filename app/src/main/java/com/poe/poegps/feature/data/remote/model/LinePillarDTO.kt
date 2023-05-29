package com.poe.poegps.feature.data.remote.model

data class LinePillarDTO(
    val tplnr: String,
    val opr: String,
    val lineName: String,
    val ucat: String,
    val pillarType: String,
    val wire: String,
    val lat: String,
    val lng: String,
    val isAbon: Boolean,
    val isSent: Boolean
)