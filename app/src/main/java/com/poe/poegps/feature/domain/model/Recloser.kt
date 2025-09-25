package com.poe.poegps.feature.domain.model

import okhttp3.internal.cache2.Relay

data class Recloser(
    val id: Int,
    val tplnr: String,
    val name: String,
    val opr: String,
    val type: String,
    val lng: String,
    val lat: String
)
