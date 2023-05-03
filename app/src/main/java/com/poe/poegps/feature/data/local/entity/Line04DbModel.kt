package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line04")
data class Line04DbModel(
    @PrimaryKey(autoGenerate = true)
    private val id: Int,
    private val tplnr: String
)
