package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pillars04")
data class Pillar04DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int =0,
    val tplnr: String,
    val name: String,
    val wire: String,
    val lng: String,
    val lat: String
)
