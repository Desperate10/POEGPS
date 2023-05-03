package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kl04")
data class Kl04DbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String
)
