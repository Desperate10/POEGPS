package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "abon_line04")
data class AbonLine04DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tplnr: String,
    val name: String
)