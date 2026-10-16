package com.poe.poegps.feature.data.remote.model

data class PillarObjects04DTO(
    val tplnr: String,
    val pltxt: String,
    // GIS leaves these empty on some poles; Gson would put null into a non-null field and the whole
    // download of this layer would fail
    val wire: String?,
    val lng: String?,
    val lat: String?

)
