package com.poe.poegps.feature.data.local.entity

import androidx.room.ColumnInfo
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
    val isAbon: Boolean,
    val isSent: Boolean = false,
    val recloser : String?= null,
    // editor order within a line; fractional so a pole can go between two others (v3)
    @ColumnInfo(defaultValue = "0") val sortOrder: Double = 0.0,
    // loaded from GIS as it is and not re-surveyed on this phone yet (v3)
    @ColumnInfo(defaultValue = "0") val fromGis: Boolean = false,
)
