package com.poe.poegps.feature.domain.repository

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import kotlinx.coroutines.flow.Flow

interface Repository {

    fun getLines(): Flow<List<Line>>

    fun getTPs(): Flow<List<Tp>>

    fun getPillars(tplnr: String): Flow<List<Pillar>>

    suspend fun saveCoordinatesOfLine(line: Line)

    suspend fun savePillars(pillars: List<Pillar>)

    suspend fun saveCoordinatesOfTp(tp: Tp)
}