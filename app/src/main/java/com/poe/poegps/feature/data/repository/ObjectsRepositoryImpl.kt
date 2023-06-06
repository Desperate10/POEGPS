package com.poe.poegps.feature.data.repository

import android.util.Log
import com.poe.poegps.feature.data.local.dao.ObjectsDao
import com.poe.poegps.feature.data.mapper.*
import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.data.remote.model.LoginRequest
import com.poe.poegps.feature.data.remote.model.SavePillarsResponse
import com.poe.poegps.feature.data.remote.model.TokenCheckResponse
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.data.remote.utils.apiRequestFlow
import com.poe.poegps.feature.domain.model.*
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObjectsRepositoryImpl @Inject constructor(
    private val objectsDao: ObjectsDao,
    private val objectsApi: ObjectsApi
) : ObjectsRepository {

    override fun auth(login: String, password: String) = apiRequestFlow {
        objectsApi.auth(LoginRequest(login, password))
    }

    override fun tokenCheck(token: String): Flow<ApiResponse<TokenCheckResponse>> {
        return apiRequestFlow {
            objectsApi.checkTokenValidity(token)
        }
    }


    override fun uploadSavedPillars(token: String): Flow<SavePillarsResponse> = flow {
        // return apiRequestFlow {
        val data = objectsDao.getSavedPillars()
        //Заменить на возврат респонса
        val response = objectsApi.uploadSavedPillars(
            token = "Bearer $token",
            pillars = data.map { it.toDTObject() })
        response.body()?.let { emit(it) }
        //  }
    }


    override fun downloadLines(filial: String, token: String): Flow<UploadState> = flow {

        try {
            emit(UploadState.Loading)

            val line04Objects = objectsApi.getLine04Objects(filial, "Bearer $token")
                .map { line ->
                    line?.toDomainModel()
                }
                .toList()

            // Обновите прогресс загрузки
            emit(UploadState.Progress(50))

            // Загрузка данных objectsApi.getLine10Objects()
            val line10Objects = objectsApi.getLine10Objects(filial, "Bearer $token")
                .map { line ->
                    line?.toDomainModel()
                }
                .toList()

            // Обновите прогресс загрузки
            emit(UploadState.Progress(50))

            // Загрузка данных objectsApi.getLine04AbonObjects()
            val response = objectsApi.getLine04AbonObjects(filial, "Bearer $token")
            val line04AbonObjects = response.map { line ->
                line?.toDomainModel()
            }.toList()

            // Обновите прогресс загрузки
            emit(UploadState.Progress(75))

            // Загрузка данных objectsApi.getLine10AbonObjects()
            val responseAbon10 = objectsApi.getLine10AbonObjects(filial, "Bearer $token")
            val line10AbonObjects = responseAbon10.map { line ->
                line?.toDomainModel()
            }.toList()

            // Обновите прогресс загрузки
            emit(UploadState.Progress(100))

            line04Objects.map { line ->
                line?.let { objectsDao.insertLine04IfNotExist(line.toLine04DbModel()) }
            }
            line10Objects.map { line ->
                line?.let { objectsDao.insertLine10IfNotExist(line.toLine10DbModel()) }
            }
            line04AbonObjects.map { line ->
                line?.let { objectsDao.insertAbonLine04IfNotExist(line.toAbonLine04DbModel()) }
            }
            line10AbonObjects.map { line ->
                line?.let { objectsDao.insertAbonLine10IfNotExist(it.toAbonLine10DbModel()) }
                // objectsDao.insertAbonLine10IfNotExist(line.toAbonLine10DbModel())
            }

            emit(UploadState.Complete)
        } catch (e: Exception) {
            emit(UploadState.Error("Помилка завантаження даних"))
        }
    }

    override suspend fun downloadTPs(filial: String, token: String) {
        objectsApi.getTpObjects(filial, "Bearer $token")
            .map { tp ->
                tp.toDomainModel()
            }
            .also { tps ->
                tps.map {
                    objectsDao.insertTpIfNotExist(it.toDbModel())
                    //objectsDao.insertTp(it.toDbModel())
                }
            }
        //abon
        objectsApi.getAbonTpObjects(filial, "Bearer $token")
            .map { tp ->
                val domain = tp.toDomainModel()
                val tps = domain.toAbonDbModel()
                //Log.d("testim", tps.toString())
                objectsDao.insertAbonTpIfNotExist(tps)
                //objectsDao.insertAbonTp(tps)
            }
        /*.also { tps ->
            tps.map {
                objectsDao.insertAbonTp(it.toAbonDbModel())
            }
        }*/
    }

    override suspend fun downloadPss(token: String) {
        objectsApi.getPsObjects("Bearer $token")
            .map { ps ->
                val domain = ps.toDomainModel()
                val pss = domain.toPsDbModel()
                objectsDao.insertPsIfNotExist(pss)
            }
        /*.also { pss ->
            pss.map {
                objectsDao.insertPs(it.toPsDbModel())
            }
        }*/
    }

    override suspend fun downloadPillars(filial: String, token: String) {
        objectsApi.getPillarObjects04(filial, "Bearer $token")
            .map { pillar ->
                pillar.toDomainModel()
            }
            .also { pillars ->
                pillars.map {
                    objectsDao.insertPillar04IfNotExist(it.to04DbModel())
                }
            }
        objectsApi.getPillarObjects10(filial, "Bearer $token")
            .map { pillar ->
                pillar.toDomainModel()
            }
            .also { pillars ->
                pillars.map {
                    objectsDao.insertPillar10IfNotExist(it.to10DbModel())
                }
            }
    }

    override suspend fun downloadWires(token: String) {
        objectsApi.getWire04("Bearer $token")
            .map { wire ->
                wire.toWire()
            }.also {
                it.map { wire ->
                    objectsDao.insertWire04IfNotExist(wire.toWire04DbModel())
                }
            }
        objectsApi.getWire10("Bearer $token")
            .map { wire ->
                wire.toWire()
            }.also {
                it.map { wire ->
                    objectsDao.insertWire10IfNotExist(wire.toWire10DbModel())
                }
            }
    }

    override fun searchLine04(tplnr: String): Flow<List<Line>> {
        return objectsDao.getLines04(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchAbonLine10(tplnr: String): Flow<List<Line>> {
        return objectsDao.getAbonLines10(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchAbonLine04(tplnr: String): Flow<List<Line>> {
        return objectsDao.getAbonLines04(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchLine10(tplnr: String): Flow<List<Line>> {
        return objectsDao.getLines10(tplnr)
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun searchTP(tplnr: String, abonState: Boolean): Flow<List<Tp>> {
        return objectsDao.getTpFlow(tplnr)
            .map { tp ->
                tp.map {
                    it.toDomainModel()
                }
            }
    }

    override fun searchPS(tplnr: String): Flow<List<Ps>> {
        return objectsDao.getPs(tplnr)
            .map { tp ->
                tp.map {
                    it.toDomainModel()
                }
            }
    }

    override fun getLineList10(): Flow<List<Line>> {
        return objectsDao.getLineList10()
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun getLineList04(): Flow<List<Line>> {
        return objectsDao.getLineList04()
            .map {
                it.map { line ->
                    line.toDomainModel()
                }
            }
    }

    override fun getTpList(isAbon: Boolean): Flow<List<Tp>> {
        return if (!isAbon) {
            objectsDao.getTps()
                .map { tp ->
                    tp.map {
                        it.toDomainModel()
                    }
                }
        } else {
            objectsDao.getAbonTps()
                .map { tp ->
                    tp.map {
                        it.toDomainModel()
                    }
                }
        }
    }

    override suspend fun getSingleTpObject(tplnr: String, isAbon: Boolean): Tp {
        return if (!isAbon) {
            objectsDao.getSingleTp(tplnr).toDomainModel()
        } else {
            objectsDao.getSingleAbonTp(tplnr).toDomainModel()
        }
    }

    override fun getPillar04List(tplnr: String): Flow<List<Pillar>> {
        return objectsDao.getPillars04ByTplnr(tplnr)
            .map { pillar ->
                pillar.map {
                    it.toDomainModel()
                }
            }
    }

    override fun getPillar10List(tplnr: String): Flow<List<Pillar>> {
        return objectsDao.getPillars10ByTplnr(tplnr)
            .map { pillar ->
                pillar.map {
                    it.toDomainModel()
                }
            }
    }

    override fun getSavedPillars(pltxt: String): Flow<List<Pillar>> {
        return objectsDao.getPillarsOfLine(pltxt)
            .map { pillar ->
                pillar.map {
                    it.toDomainModel()
                }
            }
    }

    override fun getWireList(category: String): Flow<List<Wire>> {
        return if (category.contains("0,4")) {
            objectsDao.getWire04()
                .map { wire ->
                    wire.map {
                        it.toDomainModel()
                    }
                }
        } else {
            objectsDao.getWire10()
                .map { wire ->
                    wire.map {
                        it.toDomainModel()
                    }
                }
        }
    }

    override suspend fun updateStatus(tplnrList: List<String>) {
        objectsDao.updateStatus(tplnrList)
    }

    override suspend fun savePillar(pillar: Pillar): Long {
        return objectsDao.insertSavedPillar(pillar.toLinePillarDbModel())
    }

    override suspend fun copyPillarForOtp(pillar: Pillar) {
        return objectsDao.copyPillarForOtp(pillar.toLinePillarDbModel())
    }

    override suspend fun deletePillar(pillar: Pillar) {
        objectsDao.deletePillar(pillar.toLinePillarDbModel())
    }

    override suspend fun getParentName(tplnr: String): String {
        return objectsDao.getParentName(tplnr)
    }

    override suspend fun saveLine04(line: Line) {
        objectsDao.insertOtpaika04(line.toLine04DbModel())
    }

    override suspend fun saveLine10(line: Line) {
        objectsDao.insertOtpaika10(line.toLine10DbModel())

    }

    override suspend fun saveAbonLine04(line: Line) {
        objectsDao.insertAbonOtpaika04(line.toAbonLine04DbModel())
    }

    override suspend fun saveAbonLine10(line: Line) {
        objectsDao.insertAbonOtpaika10(line.toAbonLine10DbModel())
    }

    override suspend fun saveTpCoord(pillar: Pillar) {
        objectsDao.saveNewTpCoord(pillar.toLinePillarDbModel())
    }

    override suspend fun deleteLine04(pltxt: String) {
        objectsDao.deleteLine04(pltxt)
        deleteSavedPillars(pltxt)
    }

    override suspend fun deleteLine10(pltxt: String) {
        objectsDao.deleteLine10(pltxt)
        deleteSavedPillars(pltxt)
    }

    override suspend fun deleteAbonLine04(pltxt: String) {
        objectsDao.deleteAbonLine04(pltxt)
        deleteSavedPillars(pltxt)
    }

    override suspend fun deleteAbonLine10(pltxt: String) {
        objectsDao.deleteAbonLine10(pltxt)
        deleteSavedPillars(pltxt)
    }

    private suspend fun deleteSavedPillars(pltxt: String) {
        objectsDao.deleteSavedPillars(pltxt)
    }

}