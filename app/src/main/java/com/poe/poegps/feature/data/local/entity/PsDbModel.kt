package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ps")
data class PsDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tplnr: String,
    val pltxt: String,
    val category: String,
    val lng: String? = "0.0",
    val lat: String? = "0.0"
)
