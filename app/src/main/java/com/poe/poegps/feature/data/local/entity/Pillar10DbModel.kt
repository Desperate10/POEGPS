package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pillars10")
data class Pillar10DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int =0,
    val tplnr: String,
    val name: String,
    val wire: String,
    val lng: String,
    val lat: String
)
