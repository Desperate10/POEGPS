package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line10")
data class Line10DbModel(
    @PrimaryKey(autoGenerate = true)
    private val id: Int,
    private val tplnr: String
)
