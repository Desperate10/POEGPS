package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "abon_line10")
data class AbonLine10DbModel (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tplnr: String,
    val name: String
)