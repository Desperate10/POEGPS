package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line04")
data class Line04DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val tplnr: String,
    val name: String
)
