package com.poe.poegps.feature.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kl10")
data class Kl10DbModel (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val tplnr: String
)