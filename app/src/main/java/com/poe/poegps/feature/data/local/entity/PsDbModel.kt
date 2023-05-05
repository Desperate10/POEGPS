package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ps")
data class PsDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val tplnr: String,
    val lng: String,
    val lat: String
)
