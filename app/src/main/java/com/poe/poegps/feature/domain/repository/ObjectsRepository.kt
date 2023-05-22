package com.poe.poegps.feature.domain.repository

import com.poe.poegps.feature.data.remote.model.LoginResponse
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.domain.model.Line
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Tp
import kotlinx.coroutines.flow.Flow

interface ObjectsRepository {

    //логин
    fun auth(login: String, password: String): Flow<ApiResponse<LoginResponse>>

    //загрузка линий из апи
    suspend fun downloadLines(filial: Int, token: String)

    //fun getLowLines() : Flow<List<Line>>

    //Получение линий из БД
    fun searchLine(tplnr: String): Flow<List<Line>>

    //Получение списка линий по категории
    fun getLineList10(): Flow<List<Line>>

    fun getLineList04(): Flow<List<Line>>

    //Получение списка ТП из апи
    suspend fun downloadTPs(filial: Int, token: String)

    //Получение списка ТП из БД
    fun getTpList(): Flow<List<Tp>>

    //Получение ТП из БД по клику
    fun searchTP(filial: String, tplnr: String): Flow<List<Tp>>

    //Получение списка ПС из апи
    suspend fun downloadPss(token: String)

    suspend fun getPsByTplnr(): Tp

    suspend fun downloadPillars(filial: Int, token: String)

    fun getPillarList(tplnr: String): Flow<List<Pillar>>

    fun getSavedPillars(pltxt: String): Flow<List<Pillar>>

    suspend fun savePillar(pillar: Pillar): Long

    suspend fun deletePillar(pillar: Pillar)

    suspend fun getParentName(tplnr: String): String

    fun checkTokenValidity(token: String): Flow<ApiResponse<Boolean>>

    suspend fun saveLine(line: Line)

}