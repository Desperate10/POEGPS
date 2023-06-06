package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wire04")
data class Wire04DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val name: String
)
