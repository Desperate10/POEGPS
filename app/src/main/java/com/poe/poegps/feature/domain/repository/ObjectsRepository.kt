package com.poe.poegps.feature.domain.repository

import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import kotlinx.coroutines.flow.Flow

interface ObjectsRepository {
    //загрузка линий из апи
    suspend fun downloadLines(filial: Int)

    //fun getLowLines() : Flow<List<Line>>

    //Получение линий из БД
    fun getLinesByTplnr(tplnr: String): Flow<List<Line>>

    //Получение списка ТП из апи
    suspend fun downloadTPs(filial: Int)

    //Получение списка ТП из БД
    suspend fun getTpList(): Flow<List<Tp>>

    //Получение ТП из БД по клику
    suspend fun getTpByTplnr(tplnr: String): Tp

    //Получение списка ПС из апи
    suspend fun downloadPss()

    suspend fun getPsByTplnr(): Tp

    fun getPillars(tplnr: String): Flow<List<Pillar>>

    suspend fun saveCoordinatesOfLine(line: Line)

    suspend fun savePillars(pillars: List<Pillar>)

    suspend fun saveCoordinatesOfTp(tp: Tp)
}