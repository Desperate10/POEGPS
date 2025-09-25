package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recloser")
data class RecloserDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int =0,
    val tplnr: String,
    val name: String,
    val opr: String,
    val type: String,
    val lat: String,
    val lng: String,
    val isSent: Boolean = false
)
