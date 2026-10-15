package com.poe.poegps.feature.domain.repository

import com.poe.poegps.feature.data.remote.model.LoginResponse
import com.poe.poegps.feature.data.remote.model.SavePillarsResponse
import com.poe.poegps.feature.data.remote.model.TokenCheckResponse
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ObjectsRepository {

    //логин
    fun auth(login: String, password: String): Flow<ApiResponse<LoginResponse>>

    //Проверка токена авторизации
    fun tokenCheck(token: String): Flow<ApiResponse<TokenCheckResponse>>

    //Выгрузка опор из БД
    fun uploadSavedPillars(token: String, tplnr: String = ""): Flow<SavePillarsResponse>

    //загрузка линий из апи
    fun downloadLines(filial: String, token: String): Flow<UploadState>

    //Загрузка кл/кв из апи
    suspend fun downloadKls(filial: String, token: String)

    //Получение списка ТП из апи
    suspend fun downloadTPs(filial: String, token: String)

    //Получение списка ПС из апи
    suspend fun downloadPss(token: String)

    //Получение списка опор из апи
    suspend fun downloadPillars(filial: String, token: String)

    //Получение списка проводов
    suspend fun downloadWires(token: String)

    //Получение линий 10 из БД
    fun searchLine10(tplnr: String): Flow<List<Line>>

    //Получение линий 04 из БД
    fun searchLine04(tplnr: String): Flow<List<Line>>

    //Получение абонентских линий 10 из БД
    fun searchAbonLine10(tplnr: String): Flow<List<Line>>

    //Получение абонентских линий 04 из БД
    fun searchAbonLine04(tplnr: String): Flow<List<Line>>

    //Получение кабельных линий 04 из БД
    fun searchKl04(tplnr: String): Flow<List<Line>>

    //Получение абонентских кабельных линий 04 из БД
    fun searchAbonKl04(tplnr: String): Flow<List<Line>>

    //Получение кабельных линий 6-10 из БД
    fun searchKl10(tplnr: String): Flow<List<Line>>

    //Получение абонентских кабельных линий 6-10 из БД
    fun searchAbonKl10(tplnr: String): Flow<List<Line>>

    //Получение ТП из БД по клику
    fun searchTP(tplnr: String): Flow<List<Tp>>

    //Получение абонТП из БД по клику
    fun searchAbonTP(tplnr: String): Flow<List<Tp>>

    //Получение ПС из БД по клику
    fun searchPS(tplnr: String): Flow<List<Ps>>

    //Получение списка линий по категории
    fun getLineList10(): Flow<List<Line>>

    fun getLineList04(): Flow<List<Line>>

    //Получение списка ТП из БД
    fun getTpList(isAbon: Boolean): Flow<List<Tp>>

    suspend fun getSingleTpObject(tplnr: String, isAbon: Boolean): Tp

    fun getPillar04List(tplnr: String): Flow<List<Pillar>>

    fun getPillar10List(tplnr: String): Flow<List<Pillar>>

    /** GIS poles of 6-10 kV ([highVoltage]) or 0,4 kV lines in a box; parentName is the line's name. */
    suspend fun getPillarsIn(highVoltage: Boolean, minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<Pillar>

    fun getSavedPillars(pltxt: String): Flow<List<Pillar>>

    suspend fun savePillar(pillar: Pillar): Long

    suspend fun updatePillarCoords(id: Int, lat: String, lng: String)

    suspend fun saveRecloser(recloser: Recloser) : Long

    suspend fun copyPillarForOtp(pillar: Pillar)

    suspend fun deletePillar(pillar: Pillar)

    suspend fun getParentName(tplnr: String): String

    suspend fun saveLine04(line: Line)

    suspend fun saveLine10(line: Line)

    suspend fun saveAbonLine04(line: Line)

    suspend fun saveAbonLine10(line: Line)

    suspend fun saveTpCoord(pillar: Pillar)

    suspend fun deleteLine04(pltxt: String)

    suspend fun deleteLine10(pltxt: String)

    suspend fun deleteAbonLine04(pltxt: String)

    suspend fun deleteAbonLine10(pltxt: String)

    fun getWireList(category: String): Flow<List<Wire>>
    suspend fun addRecloserToPillar(id: Int, name: String)

    fun getLineProgress(): Flow<List<LineProgress>>

    fun countUnsent(): Flow<Int>

    /** Lines (and their recloser counts) an upload will report; empty tplnr = everything unsent. */
    suspend fun getUploadGroups(tplnr: String = ""): Pair<List<UploadGroup>, Map<String, Int>>
    //suspend fun downloadReclosers(filial: String, token: String)
}