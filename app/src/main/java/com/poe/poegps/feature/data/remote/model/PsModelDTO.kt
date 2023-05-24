package com.poe.poegps.feature.data.remote.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class PsModelDTO(
    val tplnr: String,
    val pltxt: String,
    val lng: String,
    val lat: String
)
