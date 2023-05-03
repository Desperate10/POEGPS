package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tp")
data class TpDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val tplnr: String,
    val klass: String,
    val lng: String,
    val lat: String
)
