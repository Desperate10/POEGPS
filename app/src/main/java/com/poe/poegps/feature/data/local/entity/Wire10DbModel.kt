package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wire10")
data class Wire10DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val name: String
)
