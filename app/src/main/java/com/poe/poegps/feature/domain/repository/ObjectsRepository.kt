package com.poe.poegps.feature.domain.repository

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import kotlinx.coroutines.flow.Flow

interface ObjectsRepository {

    fun getLines(): Flow<List<Line>>

    fun getLowLines() : Flow<List<Line>>

    suspend fun getLine(lineName: String): Line

    fun getTPs(): Flow<List<Tp>>

    suspend fun getTp(): Tp

    fun getPss(): Flow<List<Tp>>

    suspend fun getPs(): Tp

    fun getPillars(tplnr: String): Flow<List<Pillar>>

    suspend fun saveCoordinatesOfLine(line: Line)

    suspend fun savePillars(pillars: List<Pillar>)

    suspend fun saveCoordinatesOfTp(tp: Tp)
}