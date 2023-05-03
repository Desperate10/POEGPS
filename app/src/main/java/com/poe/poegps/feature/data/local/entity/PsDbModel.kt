package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity

@Entity(tableName = "ps")
data class PsDbModel(
    val id: Int,
    val name: String,
    val tplnr: String,
    val lng: String,
    val lat: String
)
