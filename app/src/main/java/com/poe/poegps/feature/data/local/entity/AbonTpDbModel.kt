package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "abon_tp")
data class AbonTpDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tplnr: String,
    val pltxt: String,
    val lng: String,
    val lat: String
)
