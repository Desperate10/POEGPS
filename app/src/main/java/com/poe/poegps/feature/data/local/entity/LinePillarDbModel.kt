package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "line_pillar")
data class LinePillarDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tplnr: String,
    val name: String,
    val parentName:String,
    val category: String,
    val pillarType: String,
    val wire: String,
    val lat: String,
    val lng: String,
    val isAbon: Boolean
)
