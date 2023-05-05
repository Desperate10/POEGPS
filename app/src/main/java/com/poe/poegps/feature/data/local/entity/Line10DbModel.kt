package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line10")
data class Line10DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val tplnr: String,
    val name: String
)
