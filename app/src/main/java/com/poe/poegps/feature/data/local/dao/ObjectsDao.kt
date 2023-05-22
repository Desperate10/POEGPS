package com.poe.poegps.feature.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poe.poegps.feature.data.local.entity.Line04DbModel
import com.poe.poegps.feature.data.local.entity.LinePillarDbModel
import com.poe.poegps.feature.data.local.entity.PillarDbModel
import com.poe.poegps.feature.data.local.entity.PsDbModel
import com.poe.poegps.feature.data.local.entity.TpDbModel
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: Line04DbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTp(tp: TpDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPs(ps: PsDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPillar(pillar: PillarDbModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun copyPillarForOtp(pillar: LinePillarDbModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPillar(pillar: LinePillarDbModel): Long

    @Delete
    suspend fun deletePillar(pillar: LinePillarDbModel)

    @Query("SELECT * FROM tp")
    fun getTps(): Flow<List<TpDbModel>>

    @Query("SELECT * FROM line04 WHERE tplnr LIKE '%-L' || :tplnr || '%' GROUP BY name")
    fun getLines(tplnr: String): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line04 WHERE name LIKE 'ПЛ%'")
    fun getLineList10(): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line04 WHERE name LIKE 'ПЛ%'")
    fun getLineList04(): Flow<List<Line04DbModel>>

    @Query("SELECT * FROM line_pillar WHERE parentName LIKE :pltxt")
    fun getPillarsOfLine(pltxt: String): Flow<List<LinePillarDbModel>>

    @Query("SELECT * FROM pillar WHERE tplnr LIKE :tplnr")
    fun getPillarsByTplnr(tplnr: String): Flow<List<PillarDbModel>>

    @Query("SELECT * FROM tp WHERE tplnr LIKE '%-P'|| :tplnr || '%'")
    fun getTp(tplnr: String): Flow<List<TpDbModel>>

    @Query("SELECT name FROM line04 WHERE tplnr LIKE :tplnr")
    suspend fun getParentName(tplnr: String): String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtpaika(otpaika: Line04DbModel)
}